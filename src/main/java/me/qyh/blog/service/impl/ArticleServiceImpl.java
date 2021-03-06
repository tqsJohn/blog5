package me.qyh.blog.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.dao.ArticleTagDao;
import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.dao.TagDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.ArticleTag;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.widget.ArticleDateFile;
import me.qyh.blog.ui.widget.ArticleDateFiles;
import me.qyh.blog.ui.widget.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.ui.widget.ArticleSpaceFile;
import me.qyh.blog.web.interceptor.SpaceContext;
import me.qyh.util.Validators;

@Service
public class ArticleServiceImpl implements ArticleService, InitializingBean {

	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private SpaceDao spaceDao;
	@Autowired
	private ArticleTagDao articleTagDao;
	@Autowired
	private TagDao tagDao;
	@Autowired
	private ArticleIndexer articleIndexer;
	@Autowired
	private LockManager<?> lockManager;
	@Autowired
	private ArticleCache articleCache;

	private boolean rebuildIndex = true;

	private static final Logger logger = LoggerFactory.getLogger(ArticleServiceImpl.class);

	@Override
	@Transactional(readOnly = true)
	public Article getArticleForView(Integer id) {
		Article article = articleCache.getArticle(id);
		if (article != null) {
			if (article.isPublished()) {
				if (article.getIsPrivate() && UserContext.get() == null) {
					throw new AuthencationException();
				}
				return article;
			}
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true)
	public Article getArticleForEdit(Integer id) throws LogicException {
		Article article = articleCache.getArticle(id);
		if (article == null || article.isDeleted()) {
			throw new LogicException(new Message("article.notExists", "文章不存在"));
		}
		return article;
	}

	@Override
	public Article hit(Integer id) {
		Article article = articleCache.getArticle(id);
		if (article != null) {
			boolean hit = (article.isPublished() && article.getSpace().equals(SpaceContext.get()))
					? article.getIsPrivate() ? UserContext.get() != null : true : false;
			if (hit) {
				if (article.isCacheable()) {
					article.addHits();
				}
				articleDao.updateHits(id, 1);
				return article;
			}
		}
		return null;
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "articleCache", key = "'article-'+#article.id") })
	public Article writeArticle(Article article) throws LogicException {
		checkSpace(article.getSpace().getId());
		checkLock(article.getLockId());
		if (article.hasId()) {
			Article articleDb = articleDao.selectById(article.getId());
			if (articleDb == null) {
				throw new LogicException(new Message("article.notExists", "文章不存在"));
			}
			if (articleDb.isDeleted()) {
				throw new LogicException(new Message("article.deleted", "文章已经被删除"));
			}
			if (!article.isSchedule()) {
				if (article.isDraft()) {
					article.setPubDate(null);
				} else {
					article.setPubDate(articleDb.isPublished() ? articleDb.getPubDate() : new Date());
				}
			}
			article.setLastModifyDate(new Date());
			articleTagDao.deleteByArticle(articleDb);
			articleDao.update(article);
			insertTags(article);
			articleIndexer.deleteDocument(article.getId());
			if (article.isPublished()) {
				Article updated = articleDao.selectById(article.getId());
				articleIndexer.addDocument(updated);
			}
		} else {
			if (!article.isSchedule()) {
				if (article.isDraft()) {
					article.setPubDate(null);
				} else {
					article.setPubDate(new Date());
				}
			}
			articleDao.insert(article);
			insertTags(article);
			if (article.isPublished()) {
				Article updated = articleDao.selectById(article.getId());
				articleIndexer.addDocument(updated);
			}
		}
		return article;
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "articleCache", key = "'article-'+#id") })
	public void publishDraft(Integer id) throws LogicException {
		Article article = articleDao.selectById(id);
		if (article == null) {
			throw new LogicException(new Message("article.notExists", "文章不存在"));
		}
		if (!article.isDraft()) {
			throw new LogicException(new Message("article.notDraft", "文章已经被删除"));
		}
		article.setPubDate(new Date());
		article.setStatus(ArticleStatus.PUBLISHED);
		articleDao.update(article);
		articleIndexer.addDocument(article);
	}

	private void insertTags(Article article) {
		Set<Tag> tags = article.getTags();
		if (!CollectionUtils.isEmpty(tags)) {
			for (Tag tag : tags) {
				Tag tagDb = tagDao.selectByName(cleanTag(tag.getName()));
				ArticleTag articleTag = new ArticleTag();
				articleTag.setArticle(article);
				if (tagDb == null) {
					// 插入标签
					tag.setCreate(new Date());
					tag.setName(tag.getName().trim());
					tagDao.insert(tag);
					articleTag.setTag(tag);
				} else {
					articleTag.setTag(tagDb);
				}
				articleTagDao.insert(articleTag);
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Article getRandomArticle(ArticleQueryParam param) {
		return articleDao.selectByRandom(param);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "articleFilesCache")
	public ArticleDateFiles queryArticleDateFiles(Space space, ArticleDateFileMode mode, boolean queryPrivate) {
		if (mode == null) {
			mode = ArticleDateFileMode.YM;
		}
		List<ArticleDateFile> files = articleDao.selectDateFiles(space, mode, queryPrivate);
		return new ArticleDateFiles(files, mode);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "articleFilesCache")
	public List<ArticleSpaceFile> queryArticleSpaceFiles(boolean queryPrivate) {
		return articleDao.selectSpaceFiles(queryPrivate);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<Article> queryArticle(ArticleQueryParam param) {
		if (luceneQuery(param)) {
			PageResult<Integer> result = articleIndexer.query(param);
			List<Article> articles = result.hasResult() ? articleDao.selectByIds(result.getDatas())
					: new ArrayList<Article>();
			return new PageResult<Article>(param, result.getTotalRow(), articles);
		} else {
			int count = articleDao.selectCount(param);
			List<Article> datas = articleDao.selectPage(param);
			return new PageResult<Article>(param, count, datas);
		}
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "articleCache", key = "'article-'+#id") })
	public void logicDeleteArticle(Integer id) throws LogicException {
		Article article = articleDao.selectById(id);
		if (article == null) {
			throw new LogicException(new Message("article.notExists", "文章不存在"));
		}
		if (article.isDeleted()) {
			throw new LogicException(new Message("article.deleted", "文章已经被删除"));
		}
		article.setStatus(ArticleStatus.DELETED);
		articleDao.update(article);
		articleIndexer.addDocument(article);
	}

	@Override
	@Caching(evict = { @CacheEvict(value = "articleFilesCache", allEntries = true),
			@CacheEvict(value = "articleCache", key = "'article-'+#id") })
	public void recoverArticle(Integer id) throws LogicException {
		Article article = articleDao.selectById(id);
		if (article == null) {
			throw new LogicException(new Message("article.notExists", "文章不存在"));
		}
		if (!article.isDeleted()) {
			throw new LogicException(new Message("article.undeleted", "文章未删除"));
		}
		ArticleStatus status = ArticleStatus.PUBLISHED;
		if (article.getPubDate().after(new Date())) {
			status = ArticleStatus.SCHEDULED;
		}
		article.setStatus(status);

		articleDao.update(article);
		articleIndexer.addDocument(article);
	}

	@Override
	public void deleteArticle(Integer id) throws LogicException {
		Article article = articleDao.selectById(id);
		if (article == null) {
			throw new LogicException(new Message("article.notExists", "文章不存在"));
		}
		if (!article.isDraft() && !article.isDeleted()) {
			throw new LogicException(new Message("article.undeleted", "文章未删除"));
		}
		// 删除博客的引用
		articleTagDao.deleteByArticle(article);
		articleDao.deleteById(id);
		articleIndexer.deleteDocument(id);
	}

	@Override
	public List<String> getTags(String content, int max) {
		return articleIndexer.getTags(content, max);
	}

	@Override
	public String getSummary(String content, int max) {
		return articleIndexer.getSummary(content, max);
	}

	@Override
	@CacheEvict(value = "articleFilesCache", allEntries = true)
	public void pushScheduled() {
		// 查询将要发表的文章
		List<Article> articles = articleDao.selectScheduled(new Date());
		if (!articles.isEmpty()) {
			for (Article article : articles) {
				article.setStatus(ArticleStatus.PUBLISHED);
				articleDao.update(article);

				articleIndexer.addDocument(article);
			}
		}
	}

	@Transactional(readOnly = true)
	public synchronized void rebuildIndex() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		logger.debug("开始重新建立博客索引" + sdf.format(new Date()));
		long begin = System.currentTimeMillis();
		articleIndexer.deleteAll();
		List<Article> articles = articleDao.selectPublished();
		if (!CollectionUtils.isEmpty(articles)) {
			for (Article article : articles) {
				articleIndexer.addDocument(article);
			}
		}
		long end = System.currentTimeMillis();
		logger.debug("重建博客索引成功，共耗时" + (end - begin));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (rebuildIndex) {
			rebuildIndex();
		}
	}

	private boolean luceneQuery(ArticleQueryParam param) {
		return !Validators.isEmptyOrNull(param.getQuery(), true);
	}

	/**
	 * 查询标签是否存在的时候清除两边空格并且忽略大小写
	 * 
	 * @param tag
	 * @return
	 */
	protected String cleanTag(String tag) {
		return tag.trim().toLowerCase();
	}

	public void setRebuildIndex(boolean rebuildIndex) {
		this.rebuildIndex = rebuildIndex;
	}

	private void checkSpace(Integer id) throws LogicException {
		Space space = spaceDao.selectById(id);
		if (space == null) {
			throw new LogicException(new Message("space.notExists", "空间不存在"));
		}
	}

	private void checkLock(String lockId) throws LogicException {
		if (lockId != null) {
			Lock lock = lockManager.findLock(lockId);
			if (lock == null) {
				throw new LogicException(new Message("lock.notexists", "锁不存在"));
			}
		}
	}
}

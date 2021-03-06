package me.qyh.blog.web.controller.form;

import java.util.Date;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.util.Validators;

@Component
public class ArticleValidator implements Validator {

	public static final int MAX_SUMMARY_LENGTH = 500;
	private static final int MAX_TITLE_LENGTH = 50;
	public static final int MAX_CONTENT_LENGTH = 200000;
	public static final int MAX_TAG_SIZE = 10;
	private static final int[] LEVEL_RANGE = new int[] { 0, 100 };

	@Autowired
	private TagValidator tagValidator;

	@Override
	public boolean supports(Class<?> clazz) {
		return Article.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Article article = (Article) target;
		String title = article.getTitle();
		if (Validators.isEmptyOrNull(title, true)) {
			errors.reject("article.title.blank", "文章标题不能为空");
			return;
		}
		if (title.length() > MAX_TITLE_LENGTH) {
			errors.reject("article.title.toolong", new Object[] { MAX_TITLE_LENGTH },
					"文章标题不能超过" + MAX_TITLE_LENGTH + "个字符");
			return;
		}
		Set<Tag> tags = article.getTags();
		if (!CollectionUtils.isEmpty(tags)) {
			if (tags.size() > MAX_TAG_SIZE) {
				errors.reject("article.tags.oversize", new Object[] { MAX_TAG_SIZE }, "文章标签不能超过" + MAX_TAG_SIZE + "个");
				return;
			}
			for (Tag tag : tags) {
				tagValidator.validate(tag, errors);
				if (errors.hasErrors()) {
					return;
				}
			}
		}
		if (article.getIsPrivate() == null) {
			errors.reject("article.private.null", "文章私有性不能为空");
			return;
		}
		if (article.getEditor() == null) {
			errors.reject("article.editor.null", "文章编辑器不能为空");
			return;
		}
		if (article.getFrom() == null) {
			errors.reject("article.from.null", "文章标来源不能为空");
			return;
		}
		ArticleStatus status = article.getStatus();
		if (status == null) {
			errors.reject("article.status.null", "文章状态不能为空");
			return;
		}
		if (article.isDeleted()) {
			errors.reject("article.status.invalid", "无效的文章状态");
			return;
		}
		if (article.isSchedule()) {
			Date pubDate = article.getPubDate();
			if (pubDate == null) {
				errors.reject("article.pubDate.null", "文章发表日期不能为空");
				return;
			}
			if (pubDate.before(new Date())) {
				errors.reject("article.pubDate.toosmall", "文章发表日期不能小于当前日期");
				return;
			}
		}
		Space space = article.getSpace();
		if (space == null || !space.hasId()) {
			errors.reject("article.space.null", "文章所属空间不能为空");
			return;
		}
		Integer level = article.getLevel();
		if (level != null && (level < LEVEL_RANGE[0] || level > LEVEL_RANGE[1])) {
			errors.reject("article.level.error", new Object[] { LEVEL_RANGE[0], LEVEL_RANGE[1] },
					"文章级别范围应该在" + LEVEL_RANGE[0] + "和" + LEVEL_RANGE[1] + "之间");
			return;
		}
		String summary = article.getSummary();
		if (summary == null) {
			errors.reject("article.summary.blank", "文章摘要不能为空");
			return;
		}
		if (summary.length() > MAX_SUMMARY_LENGTH) {
			errors.reject("article.summary.toolong", new Object[] { MAX_SUMMARY_LENGTH },
					"文章摘要不能超过" + MAX_SUMMARY_LENGTH + "个字符");
			return;
		}
		String content = article.getContent();
		if (Validators.isEmptyOrNull(content, true)) {
			errors.reject("article.content.blank", "文章内容不能为空");
			return;
		}
		if (content.length() > MAX_CONTENT_LENGTH) {
			errors.reject("article.content.toolong", new Object[] { MAX_CONTENT_LENGTH },
					"文章内容不能超过" + MAX_CONTENT_LENGTH + "个字符");
			return;
		}
	}
}

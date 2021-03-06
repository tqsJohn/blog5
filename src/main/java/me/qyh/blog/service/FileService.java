package me.qyh.blog.service;

import java.util.List;

import me.qyh.blog.bean.BlogFilePageResult;
import me.qyh.blog.bean.BlogFileProperty;
import me.qyh.blog.bean.UploadedFile;
import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.file.FileServer;
import me.qyh.blog.file.FileStore;
import me.qyh.blog.pageparam.BlogFileQueryParam;
import me.qyh.blog.web.controller.form.BlogFileUpload;

public interface FileService {

	List<UploadedFile> upload(BlogFileUpload upload) throws LogicException;

	void createFolder(BlogFile toCreate) throws LogicException;

	BlogFilePageResult queryBlogFiles(BlogFileQueryParam param) throws LogicException;

	BlogFileProperty getBlogFileProperty(Integer id) throws LogicException;

	List<FileServer> allServers();

	void update(BlogFile toUpdate) throws LogicException;

	/**
	 * 删除文件树节点(不会删除实际的物理文件)
	 * 
	 * @see FileService#clearDeletedCommonFile()
	 * @param id
	 * @throws LogicException
	 */
	void delete(Integer id) throws LogicException;

	/**
	 * 删除待删除状态的文件<br>
	 * <strong>当前仅当物理文件删除成功后才执行删除记录操作</strong>
	 * 
	 * @see FileStore#delete(me.qyh.blog.file.CommonFile)
	 */
	void clearDeletedCommonFile();

	/**
	 * 移动节点
	 * 
	 * @param srcId
	 * @param parentId
	 * @throws LogicException 
	 */
	void move(Integer srcId, Integer parentId) throws LogicException;

}

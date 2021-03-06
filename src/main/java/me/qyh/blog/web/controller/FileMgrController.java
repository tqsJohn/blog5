package me.qyh.blog.web.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.FileServerBean;
import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.bean.UploadedFile;
import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.entity.BlogFile.BlogFileType;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.file.FileServer;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.BlogFileQueryParam;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.FileService;
import me.qyh.blog.web.controller.form.BlogFileQueryParamValidator;
import me.qyh.blog.web.controller.form.BlogFileUpload;
import me.qyh.blog.web.controller.form.BlogFileUploadValidator;
import me.qyh.blog.web.controller.form.BlogFileValidator;

@Controller
@RequestMapping("mgr/file")
public class FileMgrController extends BaseMgrController {

	@Autowired
	private FileService fileService;
	@Autowired
	private BlogFileQueryParamValidator blogFileParamValidator;
	@Autowired
	private BlogFileUploadValidator blogFileUploadValidator;
	@Autowired
	private BlogFileValidator blogFileValidator;
	@Autowired
	private ConfigService configService;

	@InitBinder(value = "blogFileQueryParam")
	protected void initBlogFileQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(blogFileParamValidator);
	}

	@InitBinder(value = "blogFileUpload")
	protected void initBlogUploadBinder(WebDataBinder binder) {
		binder.setValidator(blogFileUploadValidator);
	}

	@InitBinder(value = "blogFile")
	protected void initBlogFileBinder(WebDataBinder binder) {
		binder.setValidator(blogFileValidator);
	}

	@RequestMapping("index")
	public String index(@Validated BlogFileQueryParam blogFileQueryParam, BindingResult result, Model model) {
		if (result.hasErrors()) {
			blogFileQueryParam = new BlogFileQueryParam();
			blogFileQueryParam.setCurrentPage(1);
		}
		blogFileQueryParam.setPageSize(configService.getPageSizeConfig().getFilePageSize());
		try {
			model.addAttribute("result", fileService.queryBlogFiles(blogFileQueryParam));
			model.addAttribute("servers", fileService.allServers());
		} catch (LogicException e) {
			model.addAttribute(ERROR, e.getLogicMessage());
		}
		return "mgr/file/index";
	}

	@RequestMapping("servers")
	@ResponseBody
	public List<FileServerBean> allServers() {
		List<FileServer> servers = fileService.allServers();
		List<FileServerBean> beans = new ArrayList<FileServerBean>(servers.size());
		for (FileServer server : servers) {
			beans.add(new FileServerBean(server));
		}
		return beans;
	}

	@RequestMapping("query")
	@ResponseBody
	public JsonResult query(@Validated BlogFileQueryParam blogFileQueryParam, BindingResult result)
			throws LogicException {
		if (result.hasErrors()) {
			blogFileQueryParam = new BlogFileQueryParam();
			blogFileQueryParam.setCurrentPage(1);
		}
		blogFileQueryParam.setPageSize(configService.getPageSizeConfig().getFilePageSize());
		return new JsonResult(true, fileService.queryBlogFiles(blogFileQueryParam));
	}

	@RequestMapping(value = "upload", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult upload(@Validated BlogFileUpload blogFileUpload, BindingResult result) throws LogicException {
		if (result.hasErrors()) {
			List<ObjectError> errors = result.getAllErrors();
			for (ObjectError error : errors) {
				return new JsonResult(false,
						new Message(error.getCode(), error.getDefaultMessage(), error.getArguments()));
			}
		}
		List<UploadedFile> uploadedFiles = fileService.upload(blogFileUpload);
		return new JsonResult(true, uploadedFiles);
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("id") Integer id) throws LogicException {
		fileService.delete(id);
		return new JsonResult(true, new Message("file.delete.success", "删除成功"));
	}

	@RequestMapping(value = "{id}/pro", method = RequestMethod.GET)
	@ResponseBody
	public JsonResult pro(@PathVariable("id") int id) throws LogicException {
		return new JsonResult(true, fileService.getBlogFileProperty(id));
	}

	@RequestMapping(value = "createFolder", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult createFolder(@RequestBody @Validated BlogFile blogFile) throws LogicException {
		blogFile.setCf(null);
		blogFile.setCreateDate(new Date());
		blogFile.setType(BlogFileType.DIRECTORY);
		fileService.createFolder(blogFile);
		return new JsonResult(true, new Message("file.create.success", "创建成功"));
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@RequestBody @Validated BlogFile blogFile) throws LogicException {
		fileService.update(blogFile);
		return new JsonResult(true, new Message("file.update.success", "更新成功"));
	}

	@RequestMapping(value = "move", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult move(@RequestParam("src") Integer src, @RequestParam("parent") Integer parent)
			throws LogicException {
		fileService.move(src, parent);
		return new JsonResult(true, new Message("file.move.success", "移动成功"));
	}
}

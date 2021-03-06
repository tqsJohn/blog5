package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.PageSizeConfig;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.web.controller.form.PageSizeConfigValidator;

@RequestMapping("mgr/config/pagesize")
@Controller
public class PageSizeConfigMgrController extends BaseMgrController {

	@Autowired
	private ConfigService configService;
	@Autowired
	private PageSizeConfigValidator pageSizeConfigValidator;

	@InitBinder(value = "pageSizeConfig")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageSizeConfigValidator);
	}

	@RequestMapping("index")
	public String index(Model model) {
		model.addAttribute("pageSize", configService.getPageSizeConfig());
		return "mgr/config/pagesize";
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@Validated @RequestBody PageSizeConfig pageSizeConfig) throws LogicException {
		configService.updatePageSizeConfig(pageSizeConfig);
		return new JsonResult(true, new Message("pagesize.update.success", "更新成功"));
	}

}

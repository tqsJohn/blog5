package me.qyh.blog.web.controller;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.UserService;
import me.qyh.blog.web.controller.form.UserValidator;

@Controller
@RequestMapping("mgr/user")
public class UserMgrController extends BaseMgrController {

	@Autowired
	private UserService userMgrService;
	@Autowired
	private UserValidator userValidator;

	@InitBinder(value = "user")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(userValidator);
	}

	@RequestMapping(value = "index")
	public String index() {
		return "mgr/user/index";
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@Validated @RequestBody User user, HttpSession session) throws LogicException {
		user.setId(UserContext.get().getId());
		userMgrService.updateUser(user);
		session.setAttribute(Constants.USER_SESSION_KEY, user);
		return new JsonResult(true, new Message("user.update.success", "更新成功"));
	}

}

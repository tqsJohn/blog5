package me.qyh.blog.web.controller.form;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ExpandedPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.ui.widget.Widget;
import me.qyh.blog.ui.widget.WidgetTpl;
import me.qyh.util.Validators;

@Component
public class PageValidator implements Validator {

	public static final int PAGE_TPL_MAX_LENGTH = 500000;
	private static final int WIDGET_TPL_MAX_LENGTH = 50000;

	private static final int PAGE_NAME_MAX_LENGTH = 20;
	private static final int PAGE_DESCRIPTION_MAX_LENGTH = 500;

	@Override
	public boolean supports(Class<?> clazz) {
		return Page.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Page page = (Page) target;
		String pageTpl = page.getTpl();
		if (pageTpl == null) {
			errors.reject("page.tpl.null", "页面模板不能为空");
			return;
		}

		if (pageTpl.length() > PAGE_TPL_MAX_LENGTH) {
			errors.reject("page.tpl.toolong", new Object[] { PAGE_TPL_MAX_LENGTH },
					"页面模板不能超过" + PAGE_TPL_MAX_LENGTH + "个字符");
			return;
		}

		List<WidgetTpl> tpls = page.getTpls();
		if (!CollectionUtils.isEmpty(tpls)) {
			for (WidgetTpl tpl : tpls) {
				Widget widget = tpl.getWidget();
				if (widget == null) {
					errors.reject("page.widget.null", "挂件不能为空");
					return;
				}
				String name = widget.getName();
				if (Validators.isEmptyOrNull(name, true)) {
					errors.reject("page.widget.name.blank", "挂件名不能为空");
					return;
				}
				if (tpl.getTpl() == null) {
					errors.reject("page.widget.tpl.null", "挂件内容不能为空");
					return;
				}
				if (tpl.getTpl().length() > WIDGET_TPL_MAX_LENGTH) {
					errors.reject("page.widget.tpl.toolong", new Object[] { WIDGET_TPL_MAX_LENGTH },
							"挂件内容不能超过" + WIDGET_TPL_MAX_LENGTH + "个字符");
					return;
				}
			}
		}

		if (page instanceof SysPage) {
			SysPage sysPage = (SysPage) page;
			if (sysPage.getTarget() == null) {
				errors.reject("page.target.blank", "页面目标不能为空");
				return;
			}
		}

		if (page instanceof UserPage) {
			UserPage userPage = (UserPage) page;
			String name = userPage.getName();
			if (Validators.isEmptyOrNull(name, true)) {
				errors.reject("page.name.blank", "页面名称不能为空");
				return;
			}
			if (name.length() > PAGE_NAME_MAX_LENGTH) {
				errors.reject("page.name.toolong", "页面名称不能超过" + PAGE_NAME_MAX_LENGTH + "个字符");
				return;
			}
			String description = userPage.getDescription();
			if (description == null) {
				errors.reject("page.description.null", "页面描述不能为空");
				return;
			}
			if (description.length() > PAGE_DESCRIPTION_MAX_LENGTH) {
				errors.reject("page.description.toolong", "页面描述不能超过" + PAGE_DESCRIPTION_MAX_LENGTH + "个字符");
				return;
			}
		}

		if (page instanceof ExpandedPage) {
			ExpandedPage expandedPage = (ExpandedPage) page;
			if (!page.hasId()) {
				errors.reject("page.id.blank", "页面ID不能为空");
				return;
			}
			String name = expandedPage.getName();
			if (Validators.isEmptyOrNull(name, true)) {
				errors.reject("page.name.blank", "页面名称不能为空");
				return;
			}
			if (name.length() > PAGE_NAME_MAX_LENGTH) {
				errors.reject("page.name.toolong", "页面名称不能超过" + PAGE_NAME_MAX_LENGTH + "个字符");
				return;
			}
		}
		
		if(page instanceof ErrorPage){
			
		}
	}

}

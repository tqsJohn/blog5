package me.qyh.blog.ui.page;

public class ExpandedPage extends Page {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public final PageType getType() {
		return PageType.EXPANDED;
	}

}

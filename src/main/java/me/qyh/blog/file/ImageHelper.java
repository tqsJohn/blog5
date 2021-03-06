package me.qyh.blog.file;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import me.qyh.blog.file.local.ImageReadException;

public abstract class ImageHelper {

	public static final String GIF = "gif";
	public static final String JPEG = "jpeg";
	public static final String JPG = "jpg";
	public static final String PNG = "png";
	public static final String WEBP = "webp";

	public static final String[] IMG_EXTENSIONS = { GIF, JPEG, JPG, PNG, WEBP };

	public abstract void resize(Resize resize, File src, File dest) throws Exception;

	public abstract ImageInfo read(File file) throws ImageReadException;

	public abstract void getGifCover(File gif, File dest) throws ImageReadException;

	public abstract void format(File src, File dest) throws Exception;

	public final static boolean maybeImage(String filename) {
		String extension = FilenameUtils.getExtension(filename);
		return (GIF.equalsIgnoreCase(extension) || JPEG.equalsIgnoreCase(extension) || JPG.equalsIgnoreCase(extension)
				|| PNG.equalsIgnoreCase(extension) || WEBP.equalsIgnoreCase(extension));
	}

	public final class ImageInfo {
		private int width;
		private int height;
		private String extension;// 图片真实后缀

		public ImageInfo(int width, int height, String extension) {
			super();
			this.width = width;
			this.height = height;
			this.extension = extension;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public String getExtension() {
			return extension;
		}
	}

}

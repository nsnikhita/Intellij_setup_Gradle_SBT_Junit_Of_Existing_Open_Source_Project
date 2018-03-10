package sec;

import java.io.File;

import javax.swing.filechooser.FileFilter;

class SecretFileFilter extends FileFilter {
	public final String description;
	public final CipherHelper helper;
	
	public SecretFileFilter (CipherHelper helper, String description) {
		this.helper = helper;
		this.description = description;
	}
	
	@Override
	public String getDescription () {
		return description;
	}
	
	@Override
	public boolean accept (File f) {
		return f.isDirectory() || f.getName().toLowerCase().endsWith(".sec");
	}
}
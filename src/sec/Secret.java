package sec;

import java.io.*;
import java.util.Properties;

public class Secret {
	public static final byte TEXT_TYPE = 0;
	public static final byte RTF_TYPE = 1;
	public static final byte FILE_TYPE = 2;
	
	public static final byte KEY_INDEX = 0;
	public static final byte VALUE_INDEX = 1;
	public static final byte TYPE_INDEX = 2;
	public static final byte PROPERTIES_INDEX = 3;
	public static final byte INDEXES = 4;
	
	public String name;
	public byte[] type;
	public byte[] value;
	public byte[] properties;
	public boolean modified;
	
	public Secret (String name) {
		this(name, null, null);
	}
	
	public Secret (String name, byte[] type, byte[] value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}
	
	public Properties getProperties() {
		Properties p = new Properties();
		if (properties != null && properties.length > 0) {
			try (ByteArrayInputStream bis = new ByteArrayInputStream(properties)) {
				p.load(bis);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return p;
	}
	
	public void setProperties(Properties p) {
		if (p != null && p.size() > 0) {
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				p.store(bos, null);
				properties = bos.toByteArray();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			properties = null;
		}
	}
	
	@Override
	public String toString () {
		return name + (modified ? "*": "");
	}
	
}

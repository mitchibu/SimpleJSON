package jp.gr.java_conf.mitchibu.lib.simplejson;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import android.text.TextUtils;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

@SuppressWarnings("unused")
public class SimpleJsonParser implements Closeable {
	public static final String DIVIDER_ARRAY = "[";
	public static final String DIVIDER_OBJECT = "{";

	private final Map<String, JsonHandler> handlerMap = new HashMap<>();
	private final Stack<String> nameStack = new Stack<>();
	private final JsonReader jsonReader;

	public SimpleJsonParser(String json) {
		this(new StringReader(json));
	}

	public SimpleJsonParser(InputStream in) {
		this(new InputStreamReader(in));
	}

	public SimpleJsonParser(Reader reader) {
		if(reader == null) throw new IllegalArgumentException();
		jsonReader = new JsonReader(reader);
	}

	@Override
	public void close() throws IOException {
		jsonReader.close();
	}

	public void addHandler(String path, JsonHandler handler) {
		if(TextUtils.isEmpty(path)) throw new IllegalArgumentException();
		handlerMap.put(path, handler);
	}

	public void parse() throws Exception {
		String lastName = null;
		String name;
		JsonToken token;
		while((token = jsonReader.peek()) != JsonToken.END_DOCUMENT) {
			switch(token) {
			case BEGIN_ARRAY:
//				android.util.Log.v("test", "[");
				jsonReader.beginArray();
				lastName = lastName == null ? nameStack.push(null) : null;
				nameStack.push(DIVIDER_ARRAY);
				onBegin(currentPath());
				break;
			case BEGIN_OBJECT:
//				android.util.Log.v("test", "{");
				jsonReader.beginObject();
				lastName = lastName == null ? nameStack.push(null) : null;
				nameStack.push(DIVIDER_OBJECT);
				onBegin(currentPath());
				break;
			case BOOLEAN:
				lastName = lastName == null ? nameStack.push(null) : null;
				name = nameStack.pop();
				onValue(currentPath(), name, Boolean.toString(jsonReader.nextBoolean()));
				break;
			case NAME:
				nameStack.push(lastName = jsonReader.nextName());
//				android.util.Log.v("test", "lastName: " + lastName);
				break;
			case NULL:
				jsonReader.nextNull();
				lastName = lastName == null ? nameStack.push(null) : null;
				name = nameStack.pop();
				onValue(currentPath(), name, null);
				break;
			case NUMBER:
			case STRING:
				lastName = lastName == null ? nameStack.push(null) : null;
				name = nameStack.pop();
				onValue(currentPath(), name, jsonReader.nextString());
				break;
			case END_ARRAY:
//				android.util.Log.v("test", "]");
				jsonReader.endArray();
				onEnd(currentPath());
				nameStack.pop();
				nameStack.pop();
				break;
			case END_OBJECT:
//				android.util.Log.v("test", "}");
				jsonReader.endObject();
				onEnd(currentPath());
				nameStack.pop();
				nameStack.pop();
				break;
			default:
				lastName = lastName == null ? nameStack.push(null) : null;
				name = nameStack.pop();
				jsonReader.skipValue();
			}
		}
	}

	private void onBegin(String path) throws Exception {
		JsonHandler handler = handlerMap.get(currentPath());
		if(handler != null) handler.begin();
	}

	private void onEnd(String path) throws Exception {
		JsonHandler handler = handlerMap.get(currentPath());
		if(handler != null) handler.end();
	}

	private void onValue(String path, String name, String value) throws Exception {
		JsonHandler handler = handlerMap.get(currentPath());
		if(handler != null) handler.value(name, value);
	}

	private String currentPath() {
		StringBuilder sb = new StringBuilder();
		for(String str : nameStack) {
			if(str != null) sb.append(str);
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	public interface JsonHandler {
		void begin() throws Exception;
		void end() throws Exception;
		void value(String name, String value) throws Exception;
	}

	public static class SimpleJsonHandler implements JsonHandler {
		@Override
		public void begin() throws Exception {
		}

		@Override
		public void end() throws Exception {
		}

		@Override
		public void value(String name, String value) throws Exception {
			android.util.Log.v("test", String.format("%s=%s", name, value));
		}
	}

	public static class PathBuilder {
		private final StringBuilder sb = new StringBuilder();

		public PathBuilder addArray(String name) {
			if(!TextUtils.isEmpty(name))sb.append(name);
			sb.append(DIVIDER_ARRAY);
			return this;
		}

		public PathBuilder addObject(String name) {
			if(!TextUtils.isEmpty(name))sb.append(name);
			sb.append(DIVIDER_OBJECT);
			return this;
		}

		@Override
		public String toString() {
			return sb.toString();
		}
	}
}

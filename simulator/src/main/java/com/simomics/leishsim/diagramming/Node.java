package com.simomics.leishsim.diagramming;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Node {

	private final int id;
	private final int group;
	private final String text;

	private final Map<String, String> data;
	
	private Double xPos;
	private Double yPos;
	
	public Node(int id, int group, String text) {
		this.id = id;
		this.group = group;
		this.text = text;
		this.data = new LinkedHashMap<>();
	}
	
	public Node(int id, int group) {
		this(id, group, null);
	}

	public int getId() {
		return id;
	}
	
	public int getGroup() {
		return group;
	}
	
	public String getText() {
		return text;
	}

	public Double getxPos() {
		return xPos;
	}

	public void setxPos(double xPos) {
		this.xPos = xPos;
	}

	public Double getyPos() {
		return yPos;
	}

	public void setyPos(double yPos) {
		this.yPos = yPos;
	}
	
	public void addData(String key, String value) {
		data.put(key, value);
	}
	
	public Set<Entry<String, String>> getData() {
		return data.entrySet();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Node(");
		sb.append(id);
		if (text != null) {
			sb.append(" ");
			sb.append(text);
		}
		sb.append(")");
		return sb.toString();
	}
}

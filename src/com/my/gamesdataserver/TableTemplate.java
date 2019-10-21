package com.my.gamesdataserver;

import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.rawdbmanager.ColData;

public class TableTemplate {
	String name;
	ColData[] cols;
	
	public TableTemplate(String name, ColData[] cols) {
		this.name = name;
		this.cols = cols;
	}
	
	public String getName() {
		return name;
	}
	public ColData[] getCols() {
		return cols;
	}
	
	
}

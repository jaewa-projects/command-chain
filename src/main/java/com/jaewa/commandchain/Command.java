package com.jaewa.commandchain;

public interface Command {
	void execute(Context ctx) throws Exception;
}

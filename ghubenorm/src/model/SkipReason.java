package model;

public enum SkipReason {
	NONE,						//0 - NULL
	FORK,						//1
	HAS_PARENT,					//2
	PRIVATE,					//3
	ERROR,						//4
	UNKNOWN,					//5
	NO_LANGUAGE,				//6
	OTHER_LANGUAGE,				//7
	NO_FILES_AT_BRANCH,			//8
	NULL_INFO,					//9
	NO_CONFIG_FOUND,			//10
	TOO_MANY_FILES				//11
}

#include <windows.h>
#include <stdio.h>

int main(int argc, char * argv[]) {
	
	STARTUPINFOA si;
	PROCESS_INFORMATION pi;
	
	ZeroMemory(&si, sizeof(STARTUPINFOA));
	si.cb = sizeof(si);
	ZeroMemory(&pi, sizeof(PROCESS_INFORMATION));
	
	BOOL res = CreateProcessA(
		NULL,
		argv[1],
		NULL,
		NULL,
		FALSE,
		0,
		NULL,
		NULL,
		&si,
		&pi
	);
	
	if (!res) {
		fprintf(stderr, "Failed to CreateProcess.\n");
		exit(1);
	}
	
	return 0;
}

#include <windows.h>
#include <stdio.h>

DWORD WINAPI PrintAlternating(LPVOID param) {
    while (1) {
        printf("----\n");
        Sleep(500);
        printf("****\n");
        Sleep(500);
    }
    return 0;
}

int main() {
	
	for (;;) {
		CreateThread(NULL, 0, PrintAlternating, NULL, 0, NULL);
	}

    return 0;
}

#include <windows.h>
#include <stdio.h>

DWORD WINAPI PrintNumbers(LPVOID param) {
    char buffer[5];
    while (1) {
        for (int i = 0; i <= 9999; i++) {
            sprintf(buffer, "%04d", i);
            printf("%s\n", buffer);
            Sleep(10); // Slow down output
        }
    }
    return 0;
}

DWORD WINAPI PrintLetters(LPVOID param) {
    char buffer[5];
    while (1) {
        for (char c = 'A'; c <= 'Z'; c++) {
            sprintf(buffer, "%c%c%c%c", c, c, c, c);
            printf("%s\n", buffer);
            Sleep(10); // Slow down output
        }
    }
    return 0;
}

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
    HANDLE threads[3];

    threads[0] = CreateThread(NULL, 0, PrintNumbers, NULL, 0, NULL);
    threads[1] = CreateThread(NULL, 0, PrintLetters, NULL, 0, NULL);
    threads[2] = CreateThread(NULL, 0, PrintAlternating, NULL, 0, NULL);

    if (threads[0] == NULL || threads[1] == NULL || threads[2] == NULL) {
        printf("Failed to create threads.\n");
        return 1;
    }

    WaitForMultipleObjects(3, threads, TRUE, INFINITE);

    return 0;
}

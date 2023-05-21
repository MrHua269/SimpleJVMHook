// dllmain.cpp : 定义 DLL 应用程序的入口点。
#include "pch.h"
#include "HookSrc/include/MinHook.h"
#include "JNI/jni.h"
#include <TlHelp32.h>
#include <iostream>
#include "loader.h"
#include "classes.h"
HMODULE GetBaseAddr();
BOOL Attach = false;

void(* JVM_ArrayCopy)(JNIEnv* env, jclass ignored, jobject src, jint src_pos,jobject dst, jint dst_pos, jint length);
LPVOID* lpvoid = NULL;

PVOID unload(PVOID arg) {
    HMODULE hm = NULL;
    GetModuleHandleEx(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS |
        GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
        (LPWSTR)&unload, &hm);
    FreeLibraryAndExitThread(hm, 0);
}

void printMessage(JNIEnv* env) {
    if (Attach) {
        return;
    }
    Attach = true;
    jclass classLoaderClazz = NULL;
    classLoaderClazz = env->DefineClass(NULL, NULL, (jbyte*)classLoaderClass, classLoaderClassSize);
    jobjectArray classesData = (jobjectArray)env->CallStaticObjectMethod(classLoaderClazz, env->GetStaticMethodID(classLoaderClazz, "getByteArray", "(I)[[B"), (jint)classCount);//jniEnv->NewObjectArray(classCount, jniEnv->FindClass("[B"), NULL);
    int cptr = 0;
    for (jsize j = 0; j < classCount; j++)
    {
        jbyteArray classByteArray = env->NewByteArray(classSizes[j]);
        env->SetByteArrayRegion(classByteArray, 0, classSizes[j], (jbyte*)(classes + cptr));
        cptr += classSizes[j];
        env->SetObjectArrayElement(classesData, j, classByteArray);
    }
    jint injectResult = env->CallStaticIntMethod(classLoaderClazz, env->GetStaticMethodID(classLoaderClazz, "injectCP", "([[B)I"), classesData);
    if (injectResult)
    {
        MessageBoxA(NULL, "Error on injecting: injectResult != 0", "ELoader", MB_OK | MB_ICONERROR);
       
    }
    MessageBoxA(NULL, "Injected successfully!", "ELoader", MB_OK | MB_ICONINFORMATION);
    CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)unload, NULL, 0, NULL);
}


void HookMethod(JNIEnv* env, jclass ignored, jobject src, jint src_pos, jobject dst, jint dst_pos, jint length) {
    printMessage(env);
    MH_DisableHook(lpvoid);
    return JVM_ArrayCopy(env, ignored, src, src_pos, dst, dst_pos, length);
}

DWORD WINAPI Thread(CONST LPVOID lpParam) {
    MH_Initialize();
    uintptr_t getSystemPackage = (uintptr_t)GetBaseAddr() + 0x17A950;//  17C6F0
    lpvoid = reinterpret_cast<LPVOID*>(getSystemPackage);
    MH_CreateHook(reinterpret_cast<LPVOID*>(getSystemPackage), &HookMethod, reinterpret_cast<PVOID*>(&JVM_ArrayCopy));
    MH_EnableHook(reinterpret_cast<LPVOID*>(getSystemPackage));
    return 0;
}

BOOL APIENTRY DllMain( HMODULE hModule,
                       DWORD  ul_reason_for_call,
                       LPVOID lpReserved
                     )
{
    switch (ul_reason_for_call)
    {
    case DLL_PROCESS_ATTACH:
        CreateThread(NULL, 0, &Thread, NULL, 0, NULL);
    case DLL_THREAD_ATTACH:
    case DLL_THREAD_DETACH:
    case DLL_PROCESS_DETACH:
        break;
    }
    return TRUE;
}

HMODULE GetBaseAddr()
{
    MODULEENTRY32 modEntry;
    ZeroMemory(&modEntry, sizeof(MODULEENTRY32));
    modEntry.dwSize = sizeof(MODULEENTRY32);
    HANDLE h = NULL;
    h = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE, GetCurrentProcessId());
    if (h == NULL)
    {
        return NULL;
    }
    if (Module32First(h, &modEntry) == false)
    {
        return NULL;
    }
    while (true)
    {
        // MessageBox(NULL, modEntry.szModule, L"test", NULL);
        if (wcscmp(modEntry.szModule, L"jvm.dll") == 0)
        {
            return modEntry.hModule;
            // GetModuleHandle()
        }
        Module32Next(h, &modEntry);
        if (h == NULL)
            break;
    }
    return NULL;
}
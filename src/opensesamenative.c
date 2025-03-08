#include "dev_lukebemish_opensesame_natives_NativeImplementations.h"

JNIEXPORT jobject JNICALL Java_dev_lukebemish_opensesame_natives_NativeImplementations_nativeImplLookup
  (JNIEnv* env, jclass callingCls) {
    jclass cls = (*env)->FindClass(env, "java/lang/invoke/MethodHandles$Lookup");
    jfieldID fid = (*env)->GetStaticFieldID(env, cls, "IMPL_LOOKUP", "Ljava/lang/invoke/MethodHandles$Lookup;");
    return (*env)->GetStaticObjectField(env, cls, fid);
}

# AndroidX Security's bundled Tink references compile-time Error Prone annotations.
-dontwarn com.google.errorprone.annotations.**

# EPUB 点击翻页通过反射调用 R2WebView.setCurrentItem(item, smoothScroll)
# 复用滑动翻页的原生 pager 动画，需保留方法名。
-keepclassmembers class org.readium.r2.navigator.R2WebView {
    public void setCurrentItem(int, boolean);
}

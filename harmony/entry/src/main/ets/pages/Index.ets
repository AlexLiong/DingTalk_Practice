import { webview } from '@kit.ArkWeb';
import { promptAction } from '@kit.ArkUI';

@Entry
@Component
struct WebOnlinePage {
  webCtrl: webview.WebviewController = new webview.WebviewController();

  build() {
    Column() {
      Web({
        src: "http://10.0.2.2:5173",
        controller: this.webCtrl
      })
        .width("100%")
        .height("100%")
        .javaScriptAccess(true)
        .domStorageAccess(true)
        .databaseAccess(true)
        .fileAccess(false)
        .zoomAccess(false)
        .allowWindowOpenMethod(true)
        // 注意：是 onErrorReceive 不是 onErrorReceived
        .onErrorReceive((err) => {
            promptAction.showToast({ message: `网页加载失败` });
        })
        .onPageEnd(() => {
          console.info("页面加载完成");
        })
    }
    .width("100%")
    .height("100%")
  }
}
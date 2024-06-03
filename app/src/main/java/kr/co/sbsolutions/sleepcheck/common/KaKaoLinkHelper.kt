package kr.co.sbsolutions.sleepcheck.common

import android.content.Context
import android.util.Log
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.ItemContent
import com.kakao.sdk.template.model.ItemInfo
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.Social
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import javax.inject.Inject

class KaKaoLinkHelper @Inject constructor(
    private val context: Context
) {

    private val defaultFeed = FeedTemplate(
        content = Content(
            title = "숨이랑을 통해 나의 호흡 상태를 확인해 보세요.",
            description = "클릭해서 호흡상태 확인하기",
            imageUrl = "https://mud-kage.kakao.com/dn/Q2iNx/btqgeRgV54P/VLdBs9cvyn8BJXB3o7N8UK/kakaolink40_original.png",
            link = Link(
                webUrl = "https://developers.kakao.com",
                mobileWebUrl = "https://developers.kakao.com"
            )
        ),
        itemContent = ItemContent(
            profileText = "Kakao",
            profileImageUrl = "https://mud-kage.kakao.com/dn/Q2iNx/btqgeRgV54P/VLdBs9cvyn8BJXB3o7N8UK/kakaolink40_original.png",
            titleImageUrl = "https://mud-kage.kakao.com/dn/Q2iNx/btqgeRgV54P/VLdBs9cvyn8BJXB3o7N8UK/kakaolink40_original.png",
            titleImageText = "Cheese cake",
            titleImageCategory = "Cake",
            items = listOf(
                ItemInfo(item = "cake1", itemOp = "1000원"),
                ItemInfo(item = "cake2", itemOp = "2000원"),
                ItemInfo(item = "cake3", itemOp = "3000원"),
                ItemInfo(item = "cake4", itemOp = "4000원"),
                ItemInfo(item = "cake5", itemOp = "5000원")
            ),
            sum = "Total",
            sumOp = "15000원"
        ),
        social = Social(
            likeCount = 286,
            commentCount = 45,
            sharedCount = 845
        ),
        buttons = listOf(
            Button(
                "앱으로 보기",
                Link(
                    androidExecutionParams = mapOf("key1" to "value1", "key2" to "value2"),
                    iosExecutionParams = mapOf("key1" to "value1", "key2" to "value2")
                )
            )
        )
    )

    fun shareKaKao() {
        if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
            // 카카오톡으로 카카오톡 공유 가능
            ShareClient.instance.shareDefault(context, defaultFeed) { sharingResult, error ->
                if (error != null) {
                    Log.e(TAG, "카카오톡 공유 실패", error)
                }
                else if (sharingResult != null) {
                    Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
//                    startActivity(context,sharingResult.intent)

                    // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                    Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                    Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                }
            }
        }
    }
}
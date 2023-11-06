package moe.fuqiuluo.shamrock.remote.service.data.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class NoticeType {
    @SerialName("group_ban") GroupBan,
    @SerialName("group_admin") GroupAdminChange,
    @SerialName("group_decrease") GroupMemDecrease,
    @SerialName("group_increase") GroupMemIncrease,
    @SerialName("group_recall") GroupRecall,
    @SerialName("friend_recall") FriendRecall,
    @SerialName("notify") Notify,
    @SerialName("group_upload") GroupUpload,
    @SerialName("private_upload") PrivateUpload
}

@Serializable
internal enum class NoticeSubType {
    @SerialName("none") None,

    @SerialName("ban") Ban,
    @SerialName("lift_ban") LiftBan,

    @SerialName("set") Set,
    @SerialName("un_set") UnSet,

    @SerialName("invite") Invite,
    @SerialName("approve") Approve,
    @SerialName("leave") Leave,
    @SerialName("kick") Kick,
    @SerialName("kick_me") KickMe,

    @SerialName("poke") Poke,
}

/**
 * 不要使用继承的方式实现通用字段，那样会很难维护！
 */
@Serializable
internal data class PushNotice(
    @SerialName("time") val time: Long,
    @SerialName("self_id") val selfId: Long,
    @SerialName("post_type") val postType: PostType,
    @SerialName("notice_type") val type: NoticeType,
    @SerialName("sub_type") val subType: NoticeSubType = NoticeSubType.None,
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("operator_id") val operatorId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("sender_id") val senderId: Long = 0,
    @SerialName("duration") val duration: Int = 0,
    @SerialName("message_id") val msgId: Int,
    @SerialName("tip_text") val tip: String = "",
    @SerialName("target_id") val target: Long = 0,
    @SerialName("file") val file: GroupFileMsg? = null,
    @SerialName("private_file") val privateFile: PrivateFileMsg? = null,
)

@Serializable
internal data class GroupFileMsg(
    val id: String,
    val name: String,
    val size: Long,
    val busid: Long,
    val url: String,
)

@Serializable
internal data class PrivateFileMsg(
    val id: String,
    val name: String,
    val size: Long,
    @SerialName("sub_id") val subId: String,
    val url: String,
    val expire: Long,
)
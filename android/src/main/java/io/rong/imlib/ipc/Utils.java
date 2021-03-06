package io.rong.imlib.ipc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import io.rong.imlib.model.UserInfo;
import io.rong.message.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.utils.BitmapUtil;

/**
 * Created by tdzl2003 on 4/13/16.
 */
public class Utils {

  public static WritableMap convertMessage(Message message) {
    WritableMap ret = Arguments.createMap();
    ret.putString("senderId", message.getSenderUserId());
    ret.putString("targetId", message.getTargetId());
    ret.putString("conversationType", message.getConversationType().getName());
    ret.putString("extra", message.getExtra());
    ret.putInt("messageId", message.getMessageId());
    ret.putDouble("receivedTime", message.getReceivedTime());
    ret.putDouble("sentTime", message.getSentTime());
    ret.putMap("content", convertMessageContent(message.getContent()));
    return ret;
  }


  private static WritableMap convertMessageContent(MessageContent content) {
    WritableMap ret = Arguments.createMap();
    if (content instanceof TextMessage) {
      TextMessage textContent = (TextMessage) content;
      ret.putString("type", "text");
      ret.putString("content", textContent.getContent());
      ret.putString("extra", textContent.getExtra());
    } else if (content instanceof VoiceMessage) {
      VoiceMessage voiceContent = (VoiceMessage) content;
      ret.putString("type", "voice");
      ret.putString("uri", voiceContent.getUri().toString());
      ret.putInt("duration", voiceContent.getDuration());
      ret.putString("extra", voiceContent.getExtra());
    } else if (content instanceof ImageMessage) {
      ImageMessage imageContent = (ImageMessage) content;
      ret.putString("type", "image");
      if (imageContent.getLocalUri() != null) {
        ret.putString("imageUrl", imageContent.getLocalUri().toString());
      }
      if (imageContent.getThumUri() != null) {
        ret.putString("thumb", imageContent.getThumUri().toString());
      }
      if (imageContent.getRemoteUri() != null) {
        ret.putString("imageUrl", imageContent.getRemoteUri().toString());
      }
      ret.putString("extra", imageContent.getExtra());
    } else if (content instanceof CommandNotificationMessage) {
      CommandNotificationMessage notifyContent = (CommandNotificationMessage) content;
      ret.putString("type", "notify");
      ret.putString("name", notifyContent.getName());
      ret.putString("data", notifyContent.getData());
    } else if (content instanceof RichContentMessage) {
      RichContentMessage richContentMessage = (RichContentMessage) content;
      ret.putString("type", "rich");
      ret.putString("content", richContentMessage.getContent());
      ret.putString("extra", richContentMessage.getExtra());
      ret.putString("imageUrl", richContentMessage.getImgUrl());
      ret.putString("url", richContentMessage.getUrl());
      ret.putString("title", richContentMessage.getTitle());

    } else if (content instanceof LocationMessage) {
      LocationMessage locationMessage = (LocationMessage) content;
      ret.putString("type", "location");
      ret.putDouble("lat", locationMessage.getLat());
      ret.putDouble("lng", locationMessage.getLng());
      ret.putString("poi", locationMessage.getPoi());
      // if (locationMessage.getImgUri() != null) {
      //     ret.putString("imageUrl", locationMessage.getImgUri().toString());
      // }
      // ret.putString("base64", locationMessage.getBase64());
      ret.putString("extra", locationMessage.getExtra());
    } else if (content instanceof FileMessage) {
      FileMessage imageContent = (FileMessage) content;
      ret.putString("type", "media");
      if (imageContent.getFileUrl() != null) {
        ret.putString("mediaUrl", imageContent.getFileUrl().toString());
      }
      if (imageContent.getLocalPath() != null) {
        ret.putString("mediaUrl", imageContent.getLocalPath().toString());
      }
      ret.putString("name", imageContent.getName());
      ret.putString("contentType", imageContent.getType());
      if (imageContent.getExtra() != null) {
        try {
          JSONObject jsonObject = new JSONObject(imageContent.getExtra());
          ret.putString("thumb", jsonObject.getString("thumb"));
          ret.putString("extra", jsonObject.getString("extra"));
        } catch (JSONException e) {
          e.printStackTrace();
        }

      }
    } else {
      ret.putString("type", "unknown");
    }
    if (content != null) {
      UserInfo userInfo = content.getUserInfo();
      if (userInfo != null) {
        ret.putMap("userInfo", convertUserInfo(userInfo));
      }
    }
    return ret;
  }

  public static UserInfo convertUserInfo(ReadableMap map) {
    return new UserInfo(map.getString("userId"), map.getString("name"), Uri.parse(map.getString("portraitUri")));
  }

  @NonNull
  private static WritableMap convertUserInfo(UserInfo userInfo) {
    WritableMap userMap = Arguments.createMap();
    userMap.putString("userId", userInfo.getUserId());
    userMap.putString("name", userInfo.getName());
    if (userInfo.getPortraitUri() != null) {
      userMap.putString("portraitUri", userInfo.getPortraitUri().toString());
    }
    return userMap;
  }

  public static WritableArray convertMessageList(List<Message> messages) {
    WritableArray ret = Arguments.createArray();

    if (messages != null) {
      for (Message msg : messages) {
        ret.pushMap(convertMessage(msg));
      }
    }
    return ret;
  }

  public static WritableArray convertConversationList(List<Conversation> conversations) {
    WritableArray ret = Arguments.createArray();
    if (conversations != null) {
      for (Conversation conv : conversations) {
        ret.pushMap(convertConversation(conv));
      }
    }
    return ret;
  }

  public static WritableMap convertConversation(Conversation conv) {
    WritableMap ret = Arguments.createMap();
    if (conv == null) return ret;
    ret.putString("title", conv.getConversationTitle());
    ret.putString("conversationTitle", conv.getConversationTitle());
    ret.putBoolean("isTop", conv.isTop());
    ret.putString("type", conv.getConversationType().getName());
    ret.putString("targetId", conv.getTargetId());
    ret.putDouble("sentTime", conv.getSentTime());
    ret.putString("senderUserId", conv.getSenderUserId());
    ret.putString("senderUserName", conv.getSenderUserName());
    ret.putString("draft", conv.getDraft());
    ret.putString("portraitUrl", conv.getPortraitUrl());
    ret.putInt("unreadCount", conv.getUnreadMessageCount());
    ret.putInt("mentionedCount", conv.getMentionedCount());
    ret.putString("notificationStatus", conv.getNotificationStatus().name());
    ret.putDouble("receivedTime", conv.getReceivedTime());
    ret.putDouble("latestMessageId", conv.getLatestMessageId());
    ret.putMap("lastMessage", convertMessageContent(conv.getLatestMessage()));
    return ret;

  }

  public static void setMessageUserInfo(MessageContent messageContent, ReadableMap map) {
    if (map.hasKey("userInfo")) {
      try {
        JSONObject user = new JSONObject(map.getString("userInfo"));
        messageContent.setUserInfo(messageContent.parseJsonToUserInfo(user));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }


  public static MessageContent convertToMessageContent(ReadableMap map) {
    String type = map.getString("type");
    if (type.equals("text")) {
      TextMessage ret = TextMessage.obtain(map.getString("content"));
      if (map.hasKey("extra")) {
        ret.setExtra(map.getString("extra"));
      }
      setMessageUserInfo(ret, map);
      return ret;
    } else if (type.equals("voice")) {
      VoiceMessage ret = VoiceMessage.obtain(Uri.parse(map.getString("uri")), map.getInt("duration"));
      // ret.setBase64(map.getString("base64"));
      if (map.hasKey("extra")) {
        ret.setExtra(map.getString("extra"));
      }
      setMessageUserInfo(ret, map);
      return ret;
    } else if (type.equals("image")) {
      String uri = map.getString("imageUrl");
      ImageMessage ret = ImageMessage.obtain(Uri.parse(uri), Uri.parse(uri), map.hasKey("full") && map.getBoolean("full"));
      if (map.hasKey("extra")) {
        ret.setExtra(map.getString("extra"));
      }
      setMessageUserInfo(ret, map);
      return ret;
    } else if (type.equals("notify")) {
      CommandNotificationMessage ret = CommandNotificationMessage.obtain(map.getString("name"), map.getString("data"));
      return ret;
    } else if (type.equals("rich")) {
      RichContentMessage ret = RichContentMessage.obtain(map.getString("title"), map.getString("content"), map.getString("imageUrl"), map.getString("url"));
      if (map.hasKey("extra")) {
        ret.setExtra(map.getString("extra"));
      }
      setMessageUserInfo(ret, map);
      return ret;
    } else if ("location".equals(type)) { // 位置消息
      double lat = map.getDouble("lat");
      double lng = map.getDouble("lng");
      String poi = map.getString("poi");
      // Uri imgUri = Uri.parse(map.getString("imageUrl"));
      LocationMessage ret = LocationMessage.obtain(lat, lng, poi, null);
      if (map.hasKey("extra")) {
        ret.setExtra(map.getString("extra"));
      }
      setMessageUserInfo(ret, map);
      return ret;
    } else if ("media".equals(type)) {//多媒体消息
      FileMessage mediaMessage = FileMessage.obtain(Uri.parse(map.getString("mediaUrl")));
      if (map.hasKey("contentType")) {
        mediaMessage.setType(map.getString("contentType"));
      }
      JSONObject extra = new JSONObject();
      try {
        extra.put("__type__", "media");
        if (map.hasKey("thumb"))
          extra.put("thumb", getStringFile(Uri.parse(map.getString("thumb"))));
        if (map.hasKey("extra")) {
          extra.put("extra", map.getString("extra"));
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
      mediaMessage.setExtra(extra.toString());
      setMessageUserInfo(mediaMessage, map);
      return mediaMessage;
    } else {
      return TextMessage.obtain("[未知消息]");
    }
  }

  public interface ImageCallback {
    void invoke(@Nullable Bitmap bitmap);
  }

  public static void getImage(Uri uri, ResizeOptions resizeOptions, final ImageCallback imageCallback) {
    BaseBitmapDataSubscriber dataSubscriber = new BaseBitmapDataSubscriber() {
      @Override
      protected void onNewResultImpl(Bitmap bitmap) {
        bitmap = bitmap.copy(bitmap.getConfig(), true);
        imageCallback.invoke(bitmap);
      }

      @Override
      protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
        imageCallback.invoke(null);
      }
    };

    ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(uri);
    if (resizeOptions != null) {
      builder = builder.setResizeOptions(resizeOptions);
    }
    ImageRequest imageRequest = builder.build();

    ImagePipeline imagePipeline = Fresco.getImagePipeline();
    DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, null);
    dataSource.subscribe(dataSubscriber, UiThreadImmediateExecutorService.getInstance());
  }

  public static MessageContent convertImageMessageContent(Context context, Bitmap bmpSource) throws IOException {
    File imageFileSource = new File(context.getCacheDir(), "source.jpg");
    File imageFileThumb = new File(context.getCacheDir(), "thumb.jpg");

    FileOutputStream fosSource = new FileOutputStream(imageFileSource);

    // 保存原图。
    bmpSource.compress(Bitmap.CompressFormat.JPEG, 100, fosSource);

    // 创建缩略图变换矩阵。
    Matrix m = new Matrix();
    m.setRectToRect(new RectF(0, 0, bmpSource.getWidth(), bmpSource.getHeight()), new RectF(0, 0, 160, 160), Matrix.ScaleToFit.CENTER);

    // 生成缩略图。
    Bitmap bmpThumb = Bitmap.createBitmap(bmpSource, 0, 0, bmpSource.getWidth(), bmpSource.getHeight(), m, true);

    imageFileThumb.createNewFile();

    FileOutputStream fosThumb = new FileOutputStream(imageFileThumb);
    bmpThumb.compress(Bitmap.CompressFormat.JPEG, 60, fosThumb);

    ImageMessage imgMsg = ImageMessage.obtain(Uri.fromFile(imageFileThumb), Uri.fromFile(imageFileSource));

    return imgMsg;
  }

  public static String getStringFile(Uri fileName) {

    Bitmap bitmap;
    try {
      bitmap = BitmapUtil.getResizedBitmap(IMLibModule.context, fileName, 64, 96);
      if (bitmap != null)
        return BitmapUtil.getBase64FromBitmap(bitmap);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }
}

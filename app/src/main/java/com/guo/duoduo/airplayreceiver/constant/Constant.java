package com.guo.duoduo.airplayreceiver.constant;


/**
 * Created by guo.duoduo on 2015/8/24.
 */
public class Constant
{

    public static final String SessionId = "X_Apple_Reverse_sessionId";
    public static final String Need_sendReverse = "SendReverse";
    public static final String ReverseMsg = "ReverseMsg";

    public static final String PlayURL = "playUrl";
    public static final String Start_Pos = "startPos";


    public interface Register
    {
        public static final int FAIL = -1;
        public static final int OK = 0;
    }

    public interface Msg
    {
        public static final int Msg_Photo = 1;
        public static final int Msg_Stop = 2;
        public static final int Msg_Video_Play = 3;
    }

    public interface Target
    {
        public static final String REVERSE = "/reverse";
        public static final String PHOTO = "/photo";
        public static final String SERVER_INFO = "/server-info";
        public static final String STOP = "/stop";
        public static final String PLAY = "/play";
        public static final String SCRUB = "/scrub";
        public static final String RATE = "/rate";
    }


    public interface Status
    {
        public static final String Status_play = "playing";
        public static final String Status_stop = "stopped";
        public static final String Status_pause = "paused";
        public static final String Status_load = "loading";
    }

    public static String getServerInfoResponse(String mac)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n"
            + "<plist version=\"1.0\">\r\n" + "<dict>\r\n" + "<key>deviceid</key>\r\n"
            + "<string>" + mac + "</string>\r\n" + "<key>features</key>\r\n"
            + "<integer>119</integer>\r\n" + "<key>model</key>\r\n"
            + "<string>AppleTV2,1</string>\r\n" + "<key>protovers</key>\r\n"
            + "<string>1.0</string>\r\n" + "<key>srcvers</key>\r\n"
            + "<string>130.14</string>\r\n" + "</dict>\r\n" + "</plist>";
    }

    /**
     * 服务器发送给客户端的event
     * @param type 0 图片，1视频
     * @param sessionId
     * @param state
     * @return
     */
    public static String getEventMsg(int type, String sessionId, String state)
    {
        String category = "";
        if(type == 0)
        {
            category = "photo";
        }
        else if(type==1)
        {
            category = "video";
        }

        String bodyStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\">\n" + "<dict>\n" + "<key>category</key>\n"
            + "<string>"+ category + "</string>\n" + "<key>state</key>\n" + "<string>" + state
            + "</string>\n" + "</dict>\n" + "</plist>\n";

        String sendMsg = "POST /event HTTP/1.1\r\n" + "X-Apple-Session-ID:" + sessionId
            + "\r\n" + "Content-Type: text/x-apple-plist+xml\r\n" + "Content-Length:"
            + bodyStr.length() + "\r\n\r\n" + bodyStr;

        return sendMsg;
    }

}

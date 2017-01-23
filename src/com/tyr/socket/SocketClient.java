package com.tyr.socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.tyr.util.Debugger;

public class SocketClient {
    public Socket socket;
    public String ip;
    public int port;

    BufferedInputStream in = null;
    BufferedOutputStream out = null;

    private Thread mThread;
    private boolean mFlag = true;
    private OnReceiveNoticeListener mListener;

    public boolean isAlive = false; // 活动状态，第一次连接上为true，心跳包确认失败变为false
    public boolean isRegistered = false; // 在心跳周期，是否收到来自server的心跳确认，指定次数内确认失败即设置isAlive为false
    public int noCheckInCnt = 0; // 未收到心跳包的连续周期计数

    // 用于客户端连接指定的服务器
    public SocketClient(String ip, int port) {
        try {
            this.ip = ip;
            this.port = port;
            socket = new Socket(ip, port);
            // 流式输出, in out 不应该在每一次receive后关闭，同socket一起关闭
            out = new BufferedOutputStream(socket.getOutputStream());
            in = new BufferedInputStream(socket.getInputStream());
            
            // 启动socket 管道流的接受进程
            mThread = new Thread(new ClientThread());
            mThread.start();
            isAlive = true; // 第一次连通，生存状态设为true
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 用于服务器把客户端的socket 管道保存起来
    public SocketClient(Socket _socket) {
        try {
            socket = _socket;
            this.ip = socket.getLocalAddress().getHostAddress();
            this.port = socket.getPort();
            out = new BufferedOutputStream(socket.getOutputStream());
            in = new BufferedInputStream(socket.getInputStream());
            
            mThread = new Thread(new ClientThread());
            mThread.start();
            isAlive = true; // 第一次连通，生存状态设为true
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        try {
            // 发送一字节的urgent 包，可以用来简单的确认socket的连接状态
            // 在某些断开连接的情况下，需要发送两次才会收到异常
            socket.sendUrgentData(0xff);
            socket.sendUrgentData(0xff);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 为Notice提供一个统一的发送接口
     * 
     * 用多态的设计方式，考虑到type类型无法由其父类得到，所需需要判断其类型再转换而得到，也 可以使用Message结构体(包含type ，Notice)
     */
    public boolean send(TCPNotice _notice) {

        if (!isAlive) {
            return false;
        }
        ByteArrayOutputStream baos = null;
        DataOutputStream dos = null;

        try {
            baos = new ByteArrayOutputStream();
            dos = new DataOutputStream(baos);
            // write start tag as header
            dos.writeUTF(TCPNotice.NOTICE_START);

            if (_notice instanceof MessageNotice) {
                MessageNotice notice = (MessageNotice) _notice;
                dos.writeInt(TCPNotice.TYPE_MESSAGE);
                dos.writeUTF(notice.src);
                dos.writeUTF(notice.dest);
                dos.writeUTF(notice.msg);

            } else if (_notice instanceof FriendResponseNotice) {
                FriendResponseNotice notice = (FriendResponseNotice) _notice;
                dos.writeInt(TCPNotice.TYPE_FRIEND_RESPONSE);
                dos.writeUTF(notice.src);
                dos.writeUTF(notice.dest);
                dos.writeBoolean(notice.agree);

            } else if (_notice instanceof FriendRequestNotice) {
                FriendRequestNotice notice = (FriendRequestNotice) _notice;
                dos.writeInt(TCPNotice.TYPE_FRIEND_REQUEST);
                dos.writeUTF(notice.src);
                dos.writeUTF(notice.dest);
                dos.writeUTF(notice.msg);
            } else if (_notice instanceof LoginNotice) {
                LoginNotice notice = (LoginNotice) _notice;
                dos.writeInt(TCPNotice.TYPE_LOGIN);
                dos.writeUTF(notice.src);
                dos.writeUTF(notice.dest);

            } else if (_notice instanceof LogoutNotice) {
                LogoutNotice notice = (LogoutNotice) _notice;
                dos.writeInt(TCPNotice.TYPE_LOGOUT);
                dos.writeUTF(notice.src);
                dos.writeUTF(notice.dest);
                Debugger.logDebug("send message : logout");
                Debugger.logDebug("userid = " + notice.src);

            } else if (_notice instanceof HeartBeatNotice) {
                HeartBeatNotice notice = (HeartBeatNotice) _notice;
                dos.writeInt(TCPNotice.TYPE_HEART_BEAT);
                dos.writeUTF(notice.src);
            } else if (_notice instanceof InitNotice) {
                InitNotice notice = (InitNotice) _notice;
                dos.writeInt(TCPNotice.TYPE_INIT);
                dos.writeUTF(notice.userId);
            } else if (_notice instanceof KickNotice) {
                KickNotice notice = (KickNotice) _notice;
                dos.writeInt(TCPNotice.TYPE_KICK);
                dos.writeUTF(notice.userId);
                dos.writeUTF(notice.ip);
            }
            byte[] bytes = baos.toByteArray();
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (dos != null) {
                    dos.close();
                }
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 该方法解析顺序与 send()方法的发送顺序相对应
     * 而且TCP协议保证了指令的解析和执行都是按发送顺序线性进行的
     */
    private TCPNotice receive() {
        TCPNotice notice = null;
        ByteArrayInputStream bais = null;
        DataInputStream dis = null;

        try {
            // 设定一次从数据流中读取的长度
            // TODO 考虑传输数据过大，分包发送如何组装的情况
            byte[] data = new byte[1024];
            in.read(data);
            bais = new ByteArrayInputStream(data);
            dis = new DataInputStream(bais);

            String startTag = dis.readUTF();
            while (startTag.equals(TCPNotice.NOTICE_START)) {
                int type = dis.readInt();
                switch (type) {
                case TCPNotice.TYPE_INIT: // 初始化
                    String userId = dis.readUTF();
                    notice = new InitNotice(userId);
                    break;

                case TCPNotice.TYPE_MESSAGE: // 消息
                    String src = dis.readUTF();
                    String dest = dis.readUTF();
                    String msg = dis.readUTF();
                    notice = new MessageNotice(src, dest, msg);
                    break;

                case TCPNotice.TYPE_FRIEND_REQUEST:
                    src = dis.readUTF();
                    dest = dis.readUTF();
                    msg = dis.readUTF();
                    notice = new FriendRequestNotice(src, dest, msg);
                    break;

                case TCPNotice.TYPE_FRIEND_RESPONSE:
                    src = dis.readUTF();
                    dest = dis.readUTF();
                    Boolean agree = dis.readBoolean();
                    notice = new FriendResponseNotice(src, dest, agree);
                    break;

                case TCPNotice.TYPE_LOGIN:
                    src = dis.readUTF();
                    dest = dis.readUTF();
                    notice = new LoginNotice(src, dest);
                    break;

                case TCPNotice.TYPE_LOGOUT:
                    src = dis.readUTF();
                    dest = dis.readUTF();
                    notice = new LogoutNotice(src, dest);
                    break;

                case TCPNotice.TYPE_HEART_BEAT:
                    src = dis.readUTF();
                    notice = new HeartBeatNotice(src);
                    isRegistered = true;
                    break;

                case TCPNotice.TYPE_KICK:
                    userId = dis.readUTF();
                    String ip = dis.readUTF();
                    notice = new KickNotice(userId, ip);
                    break;

                }
                // 处理消息
                if (mListener != null) {
                    if (notice instanceof InitNotice) {
                        mListener.onInit((InitNotice) notice, SocketClient.this);
                    } else {
                        mListener.onReceive(notice);
                    }
                }
                // 获取下一个notice
                startTag = dis.readUTF();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
                if (bais != null) {
                    bais.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return notice;
    }

    public void closeSocket() {
        try {
            socket.close();
            mThread.interrupt();
            mFlag = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 该receive线程的存活期和SocketClient一致（isAlive），当socket断开，导致out 通道断开
     * Connection reset 异常，不立即告知socket已经死亡，而把mFlag = false，该线程应该
     * 在closeSocket的时机关闭。
     * 
     * send流程类似，如果send失败不立即杀死SocketClient，把消息放入离线消息队列中，等待socket
     * 自然死亡，之后如果重连成功会在onInit 中把离线消息递出。虽然说send receive失败，99%都是
     * 因为socket已经失效，也就是说这个通道已经不可用了需要重新创建一个，但是不能100%的保证，
     * 因为也有丢包什么的其他因素，这也是我们为什么不用isConnected做简单判断的原因
     */
    class ClientThread implements Runnable {
        public void run() {
            try {
                while (mFlag) {
                    Thread.sleep(500);
                    receive();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Debugger.logDebug("socket client thread interrupted");
            }
        }
    }

    public void setOnReceiveNoticeListener(OnReceiveNoticeListener listener) {
        mListener = listener;
    }

    public interface OnReceiveNoticeListener {
        public void onReceive(TCPNotice notice);

        // 仅用于server端获取客户连接后的初始化信息，仅被调用一次
        public void onInit(InitNotice initNotice, SocketClient client);
    }
}

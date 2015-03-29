package com.nemesis.minisocialnetwork;

public class Post
{
    public String headby;
    public String headtext;
    public String timestamp,likes;
    public String numlikes;
    public String fid,uid;
    public String numcomm;
    Post(String headby, String headtext, String timestamp, String numcomm, String numlikes, String fid, String uid, String likes)
    {
        this.headby=headby;
        this.headtext=headtext;
        this.numcomm=numcomm;
        this.fid=fid;
        this.likes=likes;
        this.uid=uid;
        this.numlikes=numlikes;
        this.timestamp=timestamp;
    }
}

package com.tyr.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5PasswordEncoder
{

    private MD5PasswordEncoder()
    {
        encodingAlgorithm = "MD5";
    }

    public MD5PasswordEncoder(String s)
    {
        encodingAlgorithm = s;
    }

    private String getFormattedText(byte abyte[])
    {
    	int len = abyte.length;
        StringBuilder stringbuilder = new StringBuilder(len * 2);
        int j = 0;
        do
        {
            if(j < len)
            {
            	char c;
                int l = abyte[j] >> 4 & 15;
                c = HEX_DIGITS[l];
                stringbuilder.append(c);
                
                int i = abyte[j] & 15;
                c = HEX_DIGITS[i];
                stringbuilder.append(c);
                j++;
            } 
            else
            {
                return stringbuilder.toString();
            }
        } while(true);
    }

    public static MD5PasswordEncoder instance()
    {
        if(instance == null)
            instance = new MD5PasswordEncoder();
        return instance;
    }

    public String encode(String s)
    {
    	String str = null;
    	if(s != null)
    	{
            MessageDigest messagedigest;
			try
			{
				messagedigest = MessageDigest.getInstance(encodingAlgorithm);
				messagedigest.update(s.getBytes());
	            str = getFormattedText(messagedigest.digest());
			}
			catch (NoSuchAlgorithmException e)
			{
				e.printStackTrace();
				str = "";
			}
    	}
    	else
    	{
    		str = "";
    	}
    	return str;
    }

    private static final char HEX_DIGITS[] = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
        'a', 'b', 'c', 'd', 'e', 'f'
    };
    private static MD5PasswordEncoder instance;
    private final String encodingAlgorithm;

}

package com.treesense;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TreeSense extends HttpServlet 
{
    boolean newTaskSubmitted = false;
    static boolean isProcessing = false;
    final double latDiff = 0.001516;
    final double lonDiff = 0.001610;
    Connection con = null;
    String query = "";
    String context = "";
    Calendar calendar;
    private final String RDS_HOSTNAME = "test.com";
    private final String RDS_DB_NAME = "db";
    private final String RDS_USERNAME = "user";
    private final String RDS_PASSWORD = "pass";
    private final String RDS_PORT = "3306";
    private final AWSCredentials credentials = new BasicAWSCredentials("ID", "KEY");
    private final AmazonS3 s3client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
    ExecutorService executor = Executors.newFixedThreadPool(1);
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        Map<String, String[]> queryParameters = request.getParameterMap();
        
        if (RDS_HOSTNAME != null)
        {
            try
            {
                Class.forName("com.mysql.jdbc.Driver");
                String jdbcUrl = "jdbc:mysql://" + RDS_HOSTNAME + ":" + RDS_PORT + "/" + RDS_DB_NAME + "?user=" + RDS_USERNAME + "&password=" + RDS_PASSWORD;
                con = DriverManager.getConnection(jdbcUrl);
            } 
            catch (ClassNotFoundException | SQLException e)
            {
                writer.print("Error : ");
                e.printStackTrace(writer);
            }
        }
        if(con != null)
        {
            context = queryParameters.get("context")[0];
            switch(context)
            {
                case "newcoord" :
                {
                    if(isProcessing)
                    {
                        writer.println("processing");
                        try 
                        {
                            if(!con.isClosed()) con.close();
                        }
                        catch (SQLException ex) 
                        {
                            ex.printStackTrace(writer);
                        }
                        return;
                    }
                    else isProcessing = true;
                    
                    newTaskSubmitted = true;
                    
                    String tlLat = queryParameters.get("tlLat")[0];
                    String tlLon = queryParameters.get("tlLon")[0];
                    String blLat = queryParameters.get("blLat")[0];
                    String trLon = queryParameters.get("trLon")[0];
                    double tlLatitude = 0;
                    double tlLongitude = 0;
                    double trLongitude = 0;
                    double blLatitude = 0;
                    float percent = 0.0f;
                    if(!(tlLat == null || tlLon == null || blLat == null || trLon == null))
                    {
                        tlLatitude = Double.valueOf(tlLat);
                        tlLongitude = Double.valueOf(tlLon);
                        trLongitude = Double.valueOf(trLon);
                        blLatitude = Double.valueOf(blLat);
                        
                        double current = tlLongitude;
                        
                        int count = 0, hstretch, vstretch;
                        while(current < trLongitude)
                        {
                            current += lonDiff;
                            count+=1;
                        }
                        hstretch = count;
                        count = 0;
                        current = tlLatitude;
                        while(current > blLatitude)
                        {
                            current -= latDiff;
                            count+=1;
                        }
                        vstretch = count;

                        Coord curr = new Coord(tlLatitude, tlLongitude);
                        try
                        {
                            percent = calculateGreenLand(vstretch, hstretch, curr, tlLongitude);
                        } 
                        catch (IOException ex)
                        {
                            System.out.println("Error!" + ex);
                        }
                    }
                    isProcessing = false;
                    query = "INSERT INTO TreeData(noOfTrees, greenCover, pollutionIndex, tlLat, tlLon, blLat, trLon) VALUES(-1, " + percent + ", -1, " + trimValue(tlLatitude) + ", " + trimValue(tlLongitude) + ", " + trimValue(blLatitude) + ", " + trimValue(trLongitude) + ");";
                    try
                    {
                        Statement stmt = con.createStatement();
                        stmt.execute(query);
                        stmt.close();
                    }
                    catch (SQLException ex)
                    {
                        ex.printStackTrace(writer);
                    }
                    break;
                }
                case "status":
                {
                    try 
                    {
                        writer.println(new JSONObject().put("status", String.valueOf(isProcessing)));
                    } 
                    catch (JSONException ex)
                    {
                        ex.printStackTrace(writer);
                    }
                    break;
                }
                case "getlatest":
                {
                    try 
                    {
                        JSONObject json = new JSONObject();
                        JSONArray jarray = new JSONArray();
                        
                        float tlLat, tlLon, blLat, trLon;
                        Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                        query = "SELECT * FROM TreeData;";
                        ResultSet store = stmt.executeQuery(query);
                        store.last();
                        
                        tlLat = store.getFloat("tlLat");
                        tlLon = store.getFloat("tlLon");
                        blLat = store.getFloat("blLat");
                        trLon = store.getFloat("trLon");
                        try
                        {
                            jarray.put(new JSONObject()
                                    .put("tlLat", store.getFloat("tlLat"))
                                    .put("tlLon", store.getFloat("tlLon"))
                                    .put("blLat", store.getFloat("blLat"))
                                    .put("trLon", store.getFloat("trLon"))
                                    .put("noOfTrees", store.getInt("noOfTrees"))
                                    .put("pollutionIndex", store.getFloat("pollutionIndex"))
                                    .put("greenCover", store.getFloat("greenCover")));

                            while(store.previous())
                            {
                                if(Math.abs(tlLat - store.getFloat("tlLat")) > 0.002 || Math.abs(tlLon - store.getFloat("tlLon")) > 0.002 || Math.abs(blLat - store.getFloat("blLat")) > 0.002 || Math.abs(trLon - store.getFloat("trLon")) > 0.002)
                                {
                                    break;
                                }
                                else
                                {
                                    jarray.put(new JSONObject()
                                    .put("tlLat", store.getFloat("tlLat"))
                                    .put("tlLon", store.getFloat("tlLon"))
                                    .put("blLat", store.getFloat("blLat"))
                                    .put("trLon", store.getFloat("trLon"))
                                    .put("noOfTrees", store.getInt("noOfTrees"))
                                    .put("pollutionIndex", store.getFloat("pollutionIndex"))
                                    .put("greenCover", store.getFloat("greenCover")));
                                }
                            }

                            json.put("data", jarray);
                        }
                        catch(JSONException jse)
                        {
                            jse.printStackTrace(writer);
                        }
                        writer.println(json);
                    }
                    catch (SQLException ex) 
                    {
                        ex.printStackTrace(writer);
                    }
                    break;
                }
                case "getall":
                {
                    query = "SELECT * FROM TreeData;";
                    
                    break;
                }
                default: break;
            }
        }
        try 
        {
            if(!con.isClosed()) con.close();
        }
        catch (SQLException ex) 
        {
            ex.printStackTrace(writer);
        }
    }

    String trimValue(double value)
    {
        String svalue = String.valueOf(value);
        String temp[] = svalue.split("\\.");
        if(temp[1].length() > 6)
        {
            return (temp[0] + "." + temp[1].substring(0, 6));
        }
        else
        {
            return svalue;
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() 
    {
        return "Tree Sense Server";
    }
    
    float calculateGreenLand(int vstretch, int hstretch, Coord current, double tlLongitude) throws MalformedURLException, IOException
    {
        int pixels[] = new int[1440000];
        Coord curr = current;
        int count = 0;
        int pixelColor;
        float sum = 0.0f;
        int r, g, b;
        float hsv[] = new float[3];
        BufferedImage img = null;
        int countPix = 0;
        for(int i = 0 ; i < vstretch ; i++)
        {
            for(int j = 0 ; j < hstretch ; j++)
            {
                img = ImageIO.read(new URL("https://maps.googleapis.com/maps/api/staticmap?center=" + String.valueOf(curr.latitude) + "," + String.valueOf(curr.longitude) + "&maptype=satellite&zoom=19&size=600x624&scale=2&key=AIzaSyDkYHvAuX6SvXaFyBg9iYJkC2teTfBWbf4"));
                img = img.getSubimage(0, 0, 1200, 1200);
                                    
                img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
                for(int k = 0 ; k < pixels.length ; k++)
                {
                    pixelColor = pixels[k];
                    r = (pixelColor >> 16) & 0x000000FF;
                    g = (pixelColor >> 8) & 0x000000FF;
                    b = pixelColor & 0x000000FF;
                    hsv = Color.RGBtoHSB(r, g, b, hsv);
                    //h:73-140, s:50-100, v:15-55
                    if(hsv[0] >= 0.202f && hsv[0] <= 0.4f)
                    {
                        if(hsv[1] >= 0.25f && hsv[1] <= 1.0f)
                        {
                            if(hsv[2] >= 0.15f && hsv[2] <= 0.80f){ pixels[k] = Color.WHITE.getRGB();}
                            else pixels[k] = 0;
                        }
                        else pixels[k] = 0;
                    }
                    else pixels[k] = 0;
                }
                img.setRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
                ImagePlus temp = new ImagePlus("Test", img);
                ImageProcessor processor = temp.getProcessor();
                processor.dilate();
                processor.dilate();
                processor.dilate();
                img = temp.getBufferedImage();
                img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
                for(int k = 0 ; k < pixels.length ; k++)
                {
                    pixelColor = pixels[k];
                    r = (pixelColor >> 16) & 0x000000FF;
                    g = (pixelColor >> 8) & 0x000000FF;
                    b = (pixelColor) & 0x000000FF;
                    if(((r+g+b)/3) > 10)
                    {
                        count++;
                    }
                }
                sum+=(count / 1440000.0f);
                
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(img, "png", os);
                byte[] buffer = os.toByteArray();
                os.close();
                InputStream is = new ByteArrayInputStream(buffer);
                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentLength(buffer.length);
                meta.setContentType("image/png");
                s3client.putObject(new PutObjectRequest("treesense", String.valueOf(curr.latitude) + "," + String.valueOf(curr.longitude) + ".png", is, meta));
                is.close();
               
                curr.longitude = curr.longitude + lonDiff;
            }
            curr.latitude = curr.latitude - latDiff;
            curr.longitude = tlLongitude;
        }
        return (sum / (hstretch * vstretch));
    } 
}

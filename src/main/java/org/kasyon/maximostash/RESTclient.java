package org.kasyon.maximostash;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class RESTclient 
{
	private CloseableHttpClient client = null;
	private PoolingHttpClientConnectionManager connectionManager = null;

	RESTclient()
	{
		connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(200);
		connectionManager.setDefaultMaxPerRoute(20);

		this.client = HttpClientBuilder.create().setConnectionManager(connectionManager)
				.setRetryHandler(new DefaultHttpRequestRetryHandler(7, false)).build();
	}

	public void clientClose()
	{
		try 
		{
			this.client.close();
			this.connectionManager.closeExpiredConnections();
			this.connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public int putPage(String url, String json)
	{
		HttpPut swRequest = new HttpPut(url);
		int ret = 0;

		StringEntity se = new StringEntity(json, ContentType.APPLICATION_JSON); 
		swRequest.setEntity(se);

		CloseableHttpResponse response = null;

		try 
		{
			response = this.client.execute(swRequest);

			if (response.getEntity() == null) 
			{
				throw new NullPointerException("Unable to get html");
			}

			ret = response.getStatusLine().getStatusCode();
			EntityUtils.consume(response.getEntity());
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			swRequest.releaseConnection();
		}
		
		return ret;
	}
}

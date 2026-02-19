package fib.asw.waslab01_cs;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Form;

//This code uses the Fluent API

public class SimpleFluentClient {

	private static final String URI = "http://localhost:8080/waslab01_ss/";

	public static void main(String[] args) throws Exception {

		//plo guARDO en una variable para borrar el tweet luego, esta petición me devolverá el id del tweet
		String tweet_id = Request.post(URI)
				.bodyForm(Form.form()
						.add("author", "muha")
						.add("tweet_text", "bla")
						.build())
				.addHeader("Accept", "text/plain")
				.execute()
				.returnContent()
				.asString();


		System.out.println(Request.get(URI).addHeader("Accept", "text/plain").execute().returnContent());

		System.out.println(Request.post(URI)
				.bodyForm(Form.form()
						.add("tweet_id", tweet_id)
						.build())
				.addHeader("Accept", "text/plain")
				.execute()
				.returnContent());

	}
}
package waveform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ComponentScan(basePackages = "waveform")
@EnableAutoConfiguration
@Controller
public class Application {

	@Value("${application.records.path}")
	public String recordsPath;

	@Value("${application.ffmpeg.root}")
	public String ffmpegRoot;

	@Value("${application.tmp.path}")
	public String tmpPath;

	public String waveFormResolution = "640x80";

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);		
	}

	@RequestMapping("/")
	public String welcome(Map<String, Object> model) {

		File root = new File(recordsPath);
		List<String> records = new ArrayList<String>();
		Arrays.stream(root.listFiles()).forEach(
				(record) -> records.add(record.getName()));

		model.put("records", records);
		return "index";
	}

	@ResponseBody
	@RequestMapping(path = "/samples", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ObjectNode getSamples(@RequestParam String recordName)
			throws IOException {
		/*
		 * This generates waveform in bytes (two bytes per sample) can be used
		 * to draw canvas like soundcloud
		 */
		StringBuffer command = new StringBuffer(ffmpegRoot)
				.append("\\bin\\ffmpeg.exe -i ")
				.append(recordsPath)
				.append("\\")
				.append(recordName)
				.append(" -ac 1 -filter:a aresample=8000 -map 0:a -c:a pcm_s16le -f data -");

		System.out.println(command.toString());
		Process p = null;
		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		byte[] data = null;
		try {
			p = Runtime.getRuntime().exec(command.toString());
			ByteArrayOutputStream is = new ByteArrayOutputStream();
			readStream(p.getInputStream(), is);
			p.waitFor();
			data = is.toByteArray();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ArrayNode samples = objectNode.putArray("data");

		for (int x = 0; x < data.length; x++) {
			samples.add(data[x]);
		}
		return objectNode;
	}

	@ResponseBody
	@RequestMapping(path = "/waveform", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ObjectNode getWaveForm(@RequestParam String recordName)
			throws IOException {

		String randomFileName = System.currentTimeMillis() + ".png";
		String duration="", bitrate="", stream="";
		StringBuffer command = new StringBuffer(ffmpegRoot)
				.append("\\bin\\ffmpeg.exe -i ")
				// in case there are spaces in folder name
				.append("\"")
				.append(recordsPath)
				.append("\\")
				.append(recordName)
				.append("\"")
				.append(" -filter_complex \"showwavespic=s="
						+ waveFormResolution + "\" -frames:v 1 \"")
				.append(tmpPath).append("\\").append(randomFileName)
				.append("\"");

		System.out.println(command.toString());
		Process p = null;
		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		try {
			p = Runtime.getRuntime().exec(command.toString());

			ByteArrayOutputStream is = new ByteArrayOutputStream();
			ByteArrayOutputStream es = new ByteArrayOutputStream();
			readStream(p.getInputStream(), is);
			readStream(p.getErrorStream(), es);
			p.waitFor();
			while(p.isAlive());
			
			System.out.println("stdout: " + new String(is.toByteArray()));
			String errorStream=new String(es.toByteArray());
			System.out.println("err: " + errorStream);
			
			
			Matcher m=Pattern.compile(".*Duration:\\s(.*),\\sbitrate:\\s([0-9][0-9][0-9][0-9]?)\\s.*", Pattern.DOTALL).matcher(errorStream);
			Matcher m2=Pattern.compile(".*Stream.*\\s([0-9]{5}.*kb\\/s)\\r\\n.*", Pattern.DOTALL).matcher(errorStream);
			
			if(m.matches()) {
				duration=m.group(1);
				bitrate=m.group(2);
			} else {
				System.out.println("No match... :(");
			}
			if(m2.matches()) {
				stream=m2.group(1);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		objectNode.put("fileName", randomFileName);
		objectNode.put("duration", duration);
		objectNode.put("bitrate", bitrate);
		objectNode.put("stream", stream);
		return objectNode;
	}

	/*
	@RequestMapping("/tmp/{fileName:.+}")
	public void tmp(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("fileName") String fileName) throws IOException {
		// will read a file from tmp folder and write in output stream
		InputStream is = new FileInputStream(new File(tmpPath,fileName));

		byte[] buffer = new byte[4096]; // 4kb chunk
		int read = 0;
		while (read != -1) {
			read = is.read(buffer);
			if (read != -1)
				response.getOutputStream().write(buffer, 0, read);
		}
		is.close();
	}
	
	@RequestMapping("/record/{fileName:.+}")
	public void record(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("fileName") String fileName) throws IOException {
		// will read a file from tmp folder and write in output stream
		//response.setContentType("audio/wav; name=\""+fileName+"\"");
		
		InputStream is = new FileInputStream(new File(recordsPath,fileName));
		byte[] buffer = new byte[4096]; // 4kb chunk
		int read = 0;
		int length=0;
		while (read != -1) {
			read = is.read(buffer);
			if (read != -1) {
				response.getOutputStream().write(buffer, 0, read);
				length+=read;
			}
		}
		response.setContentLength(length);
		response.setHeader("Content-Length", Integer.valueOf(length).toString());
		is.close();
	}*/

	// This should run in a seperate thread
	private void readStream(final InputStream is, final ByteArrayOutputStream baos) {
		new Thread(() -> {
			int read = 0;
			byte[] buffer = new byte[1024];

			try {
				while (read != -1) {
					read = is.read(buffer);
					if (read != -1)
						baos.write(buffer, 0, read);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
					baos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}

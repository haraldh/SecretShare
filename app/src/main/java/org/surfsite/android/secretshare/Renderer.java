package org.surfsite.android.secretshare;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v4.print.PrintHelper;
import android.text.TextPaint;
import android.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.aztec.AztecWriter;
import com.google.zxing.common.BitMatrix;
import com.tiemens.secretshare.engine.SecretShare;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Hashtable;

public class Renderer {
	private final static AztecWriter sCodeWriter = new AztecWriter();

	public static Bitmap createBitmap(String data) {
		final Hashtable<EncodeHintType, Object> hints =
				new Hashtable<EncodeHintType, Object>();
		hints.put(EncodeHintType.CHARACTER_SET, "ISO-8859-1");
		BitMatrix result;

		final int size = (int) Math.sqrt(data.length() * 8) * 10;

		result = sCodeWriter.encode(data,
				BarcodeFormat.AZTEC,
				size,
				size,
				hints);

		final int width = result.getWidth();
		final int height = result.getHeight();
		final int[] pixels = new int[width * height];

		for (int y = 0; y < height; y++) {
			final int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] =
						result.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
			}
		}

		final Bitmap bitmap =
				Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

		return bitmap;
	}

	public static void printCode(final Activity context, final String label, final String contents) {
		new AsyncTask<Void, Void, Bitmap>() {

			@Override
			protected Bitmap doInBackground(Void... params) {
				TextPaint textPaint = new TextPaint();
				textPaint.setAntiAlias(true);
				textPaint.setColor(0xFF000000);
				final int bitmapMargin = 100;//big margin is to prevent possible clipping
				final int textHeight = 28;
				textPaint.setTextSize(textHeight);
				textPaint.setTextAlign(Paint.Align.CENTER);
				final int codePadding = (int) (textPaint.descent() * 2);
				int textWidth = getTextWidth(label, textPaint);

				Bitmap codeBitmap = createBitmap(contents);
				final int width = Math.max(textWidth, codeBitmap.getWidth());
				Bitmap bmp = Bitmap.createBitmap(width + bitmapMargin * 2,
						textHeight + codeBitmap.getHeight() + codePadding * 2 + bitmapMargin * 2,
						Bitmap.Config.RGB_565);
				Canvas canvas = new Canvas(bmp);
				Paint paint = new Paint();
				paint.setStyle(Paint.Style.FILL);
				paint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);
				paint.setAntiAlias(false);
				canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);

				int centerXForAddress = bitmapMargin + width / 2;
				int y = bitmapMargin + codePadding;
				Paint codePaint = new Paint();
				codePaint.setAntiAlias(false);
				codePaint.setDither(false);
				canvas.drawBitmap(codeBitmap, centerXForAddress - codeBitmap.getWidth() / 2, y, codePaint);
				y += codePadding - textPaint.ascent();
				canvas.drawText(label, centerXForAddress, y + codeBitmap.getHeight(), textPaint);
				return bmp;
			}

			@Override
			protected void onPostExecute(final Bitmap bitmap) {
				if (bitmap != null) {
//DEBUG
//                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
//                    android.widget.ImageView view = new android.widget.ImageView(context);
//                    view.setImageBitmap(bitmap);
//                    builder.setView(view);
//                    builder.setPositiveButton(android.R.string.ok, null);
//                    builder.show();

					PrintHelper printHelper = new PrintHelper(context);
					printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
					printHelper.printBitmap(label, bitmap);
				}

			}
		}.execute();

	}


	private static int getTextWidth(String s, Paint paint) {
		Rect bounds = new Rect();
		paint.getTextBounds(s, 0, s.length(), bounds);
		return bounds.right - bounds.left;
	}

	public static ArrayList<String> wrap(String txt, int maxWidth, boolean mustFit, Paint paint) {
		int pos = 0;
		int start = pos;
		ArrayList<String> lines = new ArrayList<String>();
		while (true) {
			int i = pos;
			if (txt == null) txt = "";
			int len = txt.length();
			if (pos >= len) {
				break;
			}
			int startForLineBreak = pos;
			while (true) {
				while (i < len && txt.charAt(i) != ' ' && txt.charAt(i) != '\n') {
					i++;
				}
				int w = getTextWidth(txt.substring(startForLineBreak, i), paint);
				if (pos == startForLineBreak) {
					if (w > maxWidth) {
						if (mustFit) {
							do {
								i--;
							}
							while (getTextWidth(txt.substring(startForLineBreak, i), paint) > maxWidth);
						}
						pos = i;
						break;
					}
				}
				if (w <= maxWidth) {
					pos = i;
					if (pos >= len)
						break;
				}
				if (w > maxWidth || i >= len || txt.charAt(i) == '\n') {
					break;
				}
				i++;
			}
			int nextBreak = pos >= len ? len : ++pos;

			if (nextBreak >= txt.length()) {
				lines.add(txt.substring(start, txt.length()));
			} else {
				char c = txt.charAt(nextBreak - 1);
				if ((c == ' ') || (c == '\n')) {
					if (nextBreak - 2 < start) {
						lines.add("");
					} else {
						lines.add(txt.substring(start, nextBreak - 1));
					}
				} else {
					lines.add(txt.substring(start, nextBreak));
				}
			}
			start = pos;
		}
		return lines;
	}

	static BigInteger stringToBigInteger(String in) {
		return new BigInteger(in.getBytes());
	}

	public static String encodeShareInfo(final SecretShare.ShareInfo piece) {
		final SecretShare.PublicInfo publicInfo = piece.getPublicInfo();
		final byte[] bytePrimeModulus = publicInfo.getPrimeModulus().toByteArray();
		final byte[] byteShare = piece.getShare().toByteArray();
		final byte[] byteDescription = publicInfo.getDescription().getBytes();
		final int byteLen = 4 + 4 + 4
				+ (4 + byteDescription.length)
				+ (4 + bytePrimeModulus.length)
				+ (4 + byteShare.length);
		final ByteBuffer byteBuffer = ByteBuffer.allocate(byteLen);
		byteBuffer.putInt(piece.getX());
		byteBuffer.putInt(publicInfo.getK());
		byteBuffer.putInt(publicInfo.getN());
		byteBuffer.putInt(byteDescription.length).put(byteDescription);
		byteBuffer.putInt(bytePrimeModulus.length).put(bytePrimeModulus);
		byteBuffer.putInt(byteShare.length).put(byteShare);
		final String byteEncoded64 = Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT);

		return "ssss-android:" + piece.getX() + "/" + publicInfo.getK()
				+ ":" + publicInfo.getN() + "=" + byteEncoded64;
	}

	public static SecretShare.PublicInfo decodePublicInfo(final String buf) {
		int index64 = buf.indexOf("=") + 1;
		String substr = buf.substring(index64);
		final byte[] decodeBytes = Base64.decode(substr, Base64.DEFAULT);
		final ByteBuffer byteBuffer = ByteBuffer.wrap(decodeBytes);
		int x = byteBuffer.getInt();
		int k = byteBuffer.getInt();
		int n = byteBuffer.getInt();
		int byteDescriptionLength = byteBuffer.getInt();
		final byte[] byteDescription = new byte[byteDescriptionLength];
		byteBuffer.get(byteDescription);
		int bytePrimeModulusLength = byteBuffer.getInt();
		final byte[] bytePrimeModulus = new byte[bytePrimeModulusLength];
		byteBuffer.get(bytePrimeModulus);
		BigInteger inPrimeModulus = new BigInteger(bytePrimeModulus);
		return new SecretShare.PublicInfo(n, k, inPrimeModulus, new String(byteDescription));
	}

	public static SecretShare.ShareInfo decodeShareInfo(final String buf, final SecretShare.PublicInfo publicInfo) throws InvalidParameterException {
		int index64 = buf.indexOf("=") + 1;
		final ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.decode(buf.substring(index64), Base64.DEFAULT));
		int x = byteBuffer.getInt();
		int k = byteBuffer.getInt();
		int n = byteBuffer.getInt();

		if (n != publicInfo.getN()) {
			throw new InvalidParameterException("SecretShare.PublicInfo.N does not match.");
		}

		if (k != publicInfo.getK()) {
			throw new InvalidParameterException("SecretShare.PublicInfo.K does not match.");
		}

		if (x > n) {
			throw new InvalidParameterException("SecretShare x > n.");
		}

		int byteDescriptionLength = byteBuffer.getInt();
		final byte[] byteDescription = new byte[byteDescriptionLength];
		byteBuffer.get(byteDescription);
		if (publicInfo.getDescription().compareTo(new String(byteDescription)) != 0) {
			throw new InvalidParameterException("SecretShare.PublicInfo.Description does not match.");
		}
		int bytePrimeModulusLength = byteBuffer.getInt();
		final byte[] bytePrimeModulus = new byte[bytePrimeModulusLength];
		byteBuffer.get(bytePrimeModulus);
		BigInteger inPrimeModulus = new BigInteger(bytePrimeModulus);
		if (inPrimeModulus.compareTo(publicInfo.getPrimeModulus()) != 0) {
			throw new InvalidParameterException("SecretShare.PublicInfo.PrimeModulus does not match.");
		}
		int byteShareLength = byteBuffer.getInt();
		final byte[] byteShare = new byte[byteShareLength];
		byteBuffer.get(byteShare);
		BigInteger inShare = new BigInteger(byteShare);
		return new SecretShare.ShareInfo(x, inShare, publicInfo);
	}

}

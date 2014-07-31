package org.surfsite.android.secretshare;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.tiemens.secretshare.engine.SecretShare;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
	public ApplicationTest() {
		super(Application.class);
	}

	public void testEncrypt() throws Exception {
		List<SecretShare.ShareInfo> pieces;
		int n = 16;
		int k = 4;
		String cleartext
				= "01234567890123456789012345678901234567890123456789"
				+ "01234567890123456789012345678901234567890123456789"
				+ "01234567890123456789012345678901234567890123456789"
				+ "01234567890123456789012345678901234567890123456789"
				+ "01234567890123456789012345678901234567890123456789"
				+ "01234567890123456789012345678901234567890123456789"
				+ "01234567890123456789012345678901234567890123456789"
				+ "01234567890123456789012345678901234567890123456789"
				+ "01234567890123456789012345678901234567890123456789"
				+ "01234567890123456789012345678901234567890123456789";
		SecretShare.PublicInfo publicInfo;
		SecretShare.PublicInfo pi = null;
		List<SecretShare.ShareInfo> si = new ArrayList<SecretShare.ShareInfo>();

		final BigInteger secretInteger = Renderer.stringToSecret(cleartext);
		final BigInteger modulus;

		modulus = SecretShare.createAppropriateModulusForSecret(secretInteger);
		publicInfo = new SecretShare.PublicInfo(n,
				k,
				modulus,
				"ssss-test€€?=)(/&%$§!#ﬁ#£ ^ﬁÌ‰)SDGFHKLŒŒﬂıÓ‚€ƒ€Ω†⁄ø⁄");
		final SecretShare.SplitSecretOutput splitSecretOutput = new SecretShare(publicInfo)
				.split(secretInteger);
		pieces = splitSecretOutput.getShareInfos();

		for (int i = 0; i < pieces.size(); i++) {
			final SecretShare.ShareInfo piece = pieces.get(i);

			final String data = Renderer.encodeShareInfo(piece);

			if (pi == null)
				pi = Renderer.decodePublicInfo(data);

			if (i % 4 == 0)
				si.add(Renderer.decodeShareInfo(data, pi));
		}

		final SecretShare.CombineOutput combineOutput = new SecretShare(pi)
				.combine(si);

		assertEquals(cleartext, Renderer.secretToString(combineOutput.getSecret()));
		assertEquals(publicInfo.getDescription(), si.get(0).getPublicInfo().getDescription());
	}
}
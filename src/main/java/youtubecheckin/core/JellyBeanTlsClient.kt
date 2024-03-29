package youtubecheckin.core

import org.bouncycastle.tls.*
import org.bouncycastle.tls.crypto.TlsCrypto
import org.bouncycastle.util.Arrays
import java.io.IOException
import java.util.*

/**
 * Spoofs the SSL Handshake of a Jelly Bean device.
 *
 * @author patrick
 */
internal class JellyBeanTlsClient(crypto: TlsCrypto?) : DefaultTlsClient(crypto) {
    @Throws(IOException::class)
    override fun getAuthentication(): TlsAuthentication {
        return DefaultTlsAuthentication(selectedCipherSuite)
    }

    override fun getClientVersion(): ProtocolVersion {
        return ProtocolVersion.TLSv10
    }

    @Throws(IOException::class)
    override fun getClientExtensions(): Hashtable<*, *> {
        val ret = OrderedHashtable()
        val clientVersion = context.clientVersion

        /*
		 * RFC 5246 7.4.1.4.1. Note: this extension is not meaningful for TLS
		 * versions prior to 1.2. Clients MUST NOT offer it if they are offering
		 * prior versions.
		 */if (TlsUtils.isSignatureAlgorithmsExtensionAllowed(clientVersion)) {
            supportedSignatureAlgorithms = getSupportedSignatureAlgorithms()
            TlsUtils.addSignatureAlgorithmsExtension(
                ret,
                supportedSignatureAlgorithms
            )
        }
        if (TlsECCUtils.containsECCipherSuites(cipherSuites)) {
            /*
			 * RFC 4492 5.1. A client that proposes ECC cipher suites in its
			 * ClientHello message appends these extensions (along with any others),
			 * enumerating the curves it supports and the point formats it can parse.
			 * Clients SHOULD send both the Supported Elliptic Curves Extension and
			 * the Supported Point Formats Extension.
			 */
            namedCurves = intArrayOf(
                NamedCurve.sect571r1,
                NamedCurve.sect571k1, NamedCurve.secp521r1, NamedCurve.sect409k1,
                NamedCurve.sect409r1, NamedCurve.secp384r1, NamedCurve.sect283k1,
                NamedCurve.sect283r1, NamedCurve.secp256k1, NamedCurve.secp256r1,
                NamedCurve.sect239k1, NamedCurve.sect233k1, NamedCurve.sect233r1,
                NamedCurve.secp224k1, NamedCurve.secp224r1, NamedCurve.sect193r1,
                NamedCurve.sect193r2, NamedCurve.secp192k1, NamedCurve.secp192r1,
                NamedCurve.sect163k1, NamedCurve.sect163r1, NamedCurve.sect163r2,
                NamedCurve.secp160k1, NamedCurve.secp160r1, NamedCurve.secp160r2
            )
            clientECPointFormats = shortArrayOf(
                ECPointFormat.uncompressed,
                ECPointFormat.ansiX962_compressed_prime,
                ECPointFormat.ansiX962_compressed_char2
            )
            TlsECCUtils.addSupportedPointFormatsExtension(ret, clientECPointFormats)
            TlsECCUtils.addSupportedEllipticCurvesExtension(ret, namedCurves)
        }
        return ret
    }

    override fun getCipherSuites(): IntArray {
        return Arrays.clone(SUITES)
    }

    override fun notifySessionID(sessionID: ByteArray) {
        super.notifySessionID(sessionID)
    }

    override fun getSessionToResume(): TlsSession? {
        return null
    }

    companion object {
        private val SUITES = intArrayOf(
            CipherSuite.TLS_RSA_WITH_RC4_128_MD5,
            CipherSuite.TLS_RSA_WITH_RC4_128_SHA,
            CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_ECDH_ECDSA_WITH_RC4_128_SHA,
            CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_ECDH_RSA_WITH_RC4_128_SHA,
            CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_DHE_DSS_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_DHE_DSS_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA,
            CipherSuite.TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,
            CipherSuite.TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,
            CipherSuite.TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,
            CipherSuite.TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA,
            CipherSuite.TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA,
            CipherSuite.TLS_RSA_WITH_DES_CBC_SHA,
            CipherSuite.TLS_DHE_RSA_WITH_DES_CBC_SHA,
            CipherSuite.TLS_DHE_DSS_WITH_DES_CBC_SHA,
            CipherSuite.TLS_RSA_EXPORT_WITH_RC4_40_MD5,
            CipherSuite.TLS_RSA_EXPORT_WITH_DES40_CBC_SHA,
            CipherSuite.TLS_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA,
            CipherSuite.TLS_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA,
            CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV
        )
    }
}
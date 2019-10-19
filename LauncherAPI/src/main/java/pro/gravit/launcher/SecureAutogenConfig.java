package pro.gravit.launcher;

import java.nio.charset.StandardCharsets;

public class SecureAutogenConfig {
    public final byte[][] certificates;

    public SecureAutogenConfig() {
        //Пока не реализован SecureLauncherConfigurator
        certificates = new byte[][] {
                ("-----BEGIN CERTIFICATE-----\n" +
                        "MIIFyjCCA7KgAwIBAgIRALnsjNjfvOTXfla3fX1fNEUwDQYJKoZIhvcNAQELBQAw\n" +
                        "WTELMAkGA1UEBhMCUlUxFzAVBgNVBAoTDkdyYXZpdFRydXN0IENBMRAwDgYDVQQL\n" +
                        "EwdSb290IENBMR8wHQYDVQQDExZHcmF2aXQgQ2VudHJhbCBSb290IENBMCAXDTE5\n" +
                        "MDYwOTAyNDIwMFoYDzIwNTEwNjA5MDI0MjAwWjBZMQswCQYDVQQGEwJSVTEXMBUG\n" +
                        "A1UEChMOR3Jhdml0VHJ1c3QgQ0ExEDAOBgNVBAsTB1Jvb3QgQ0ExHzAdBgNVBAMT\n" +
                        "FkdyYXZpdCBDZW50cmFsIFJvb3QgQ0EwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
                        "ggIKAoICAQDA3Qm9OH8Xz3YM3bKkZuQI7T/aL3ulMOdY5GFADYgHrOVZXVSJi/4P\n" +
                        "PruBsut4WXN6TGQdpJtNZ2kyWTYzENGTm/TMzBcIchor1M3JW5Uv/C0r5gSEU1uP\n" +
                        "DPe7oEpeKtb3FXML/pGoGpLv/sonTKky4AKZnK7B15bZ+oVZNwh7UKANpNrVA8k5\n" +
                        "0gb4BisFcegLidYL9Y00H1x5WzUxldQAA1IQuwdkL3NP0NPQrSVJ2Ka2EtebE2HP\n" +
                        "fXHtbftvvnvSWyh4CXAxTfEmJgut0gSPQPm9wVt6pIWWd4O0hHwVmxkKQidgnP6A\n" +
                        "+d05FnJGsBw0ztMCifIteqNiHF0D8E0GuSz6NtcuV47J3p43qkvKr2vPc8o6WMN8\n" +
                        "PAb0eVHc/AX8qqOwYQyHlj4M0SDhCltHeeYRWmuZmRFIIelv6VAocaQLlPQrhJNp\n" +
                        "feIzmXLy60a+84vpe/eQKQx+D8a1elarQkoHMxI7x/9AJvxcnJ4KuXc2rkiu3Zv9\n" +
                        "KMhixtkLc+pA6jY023U211v+c20RjTqwKIZoMFc7BZipoinAOn1bdsTzXlhOMv1O\n" +
                        "zj5WoW6DsQQONMZNyLQAkaX6SYZE/kQVJ9YMPhNdaXjxxzfrY05IrWAaWhtPbW8z\n" +
                        "5nb4/JyO+bJq3v2rav9p03s8P/lQ4k/0af5vOkGkEO0+YKx97ZP8FQIDAQABo4GK\n" +
                        "MIGHMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFFjMGCvHXAE/vGJih+Lfdo2s\n" +
                        "YnzsMAsGA1UdDwQEAwIBBjA1BgNVHR8ELjAsMCqgKKAmhiRodHRwOi8vY2EuZ3Jh\n" +
                        "dml0LnByby9jZW50cmFscm9vdC5jcmwwEQYJYIZIAYb4QgEBBAQDAgAHMA0GCSqG\n" +
                        "SIb3DQEBCwUAA4ICAQAexCGpThx85skEllva1UskmdlRh3rud9u59AUiwNZF0b0I\n" +
                        "+7eeyLNaLHarg2Zm30TSCF53ksyPTE5QNdmozs1fl3MddFqunkbUm4G6hwedZMSi\n" +
                        "4IXIb2QK3z3gZG5ZNdHaDG2u00Jdkc39h3jQFp1rpn4+0DcnYJAe+lw5G+XHURY2\n" +
                        "j15wcmUFp/Ywgw3pfCWmH5+rxq21e/LG8JiQrxekkFI2GUD+Qw7+Hq3o1Fgg3kfh\n" +
                        "Lg4B5WEbEICQ1FC+dHYHasEI3q3c96Qpqu2k3pO0l1fr6Cys+AGjoI2WrgXkGlmA\n" +
                        "F+Wi2ndoZbvspGAwxmrNMtLE3OYNuMXFF410QSPf4o9QqpGDC3a2mccTXb231a18\n" +
                        "5vDJixeZpuzEm5ECXg8j6aj53X3rtm7C8yfOsg5UTKJJj+pSNz4YTp91IDHm0nTP\n" +
                        "2KhrgS7jujgKdJn9xv07e/API3kLWkVmMwHBiaSCIaHOfAN0RJMQVV+YgnSp2sIa\n" +
                        "OATWgSKH0qTkleE/v7k+USs0a+KV8wmC5wwliqH+uLO++yIP/9bjDctyLulQX5Ee\n" +
                        "+EhD7tb1R/yyWY4uhkzlsr3N2Kl34aQAEBMn8Z1mHsyyu1FcbEaNLU8jcS3pHPVM\n" +
                        "gQRn3m1iDnQlFciAMxW0pW6mW/4xKYzhXk5BTSolnqMVylxHgWXuBwdDDQQVnQ==\n" +
                        "-----END CERTIFICATE-----").getBytes(StandardCharsets.US_ASCII)
                // ? Какая из них, но выбрать надо однозачно
        };
    }
}

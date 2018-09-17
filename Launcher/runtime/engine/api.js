var Launcher = LauncherClass.static;
var LauncherConfig = LauncherConfigClass.static;
var HTTPRequest = HTTPRequestClass.static;

// Hasher class API imports
var PlayerProfile = PlayerProfileClass.static;
var PlayerProfileTexture = PlayerProfileTextureClass.static;
var ClientProfile = ClientProfileClass.static;
var ClientProfileVersion = ClientProfileVersionClass.static;
var ClientLauncher = ClientLauncherClass.static;
var ClientLauncherParams = ClientLauncherParamsClass.static;
var ServerPinger = ServerPingerClass.static;

// Request class API imports
var Request = RequestClass.static;
var RequestType = RequestTypeClass.static;
var RequestException = RequestExceptionClass.static;
var CustomRequest = CustomRequestClass.static;
var PingRequest = PingRequestClass.static;
var AuthRequest = AuthRequestClass.static;
var JoinServerRequest = JoinServerRequestClass.static;
var CheckServerRequest = CheckServerRequestClass.static;
var UpdateRequest = UpdateRequestClass.static;
var LauncherRequest = LauncherRequestClass.static;
var ProfileByUsernameRequest = ProfileByUsernameRequestClass.static;
var ProfileByUUIDRequest = ProfileByUUIDRequestClass.static;
var BatchProfileByUsernameRequest = BatchProfileByUsernameRequestClass.static;

// Hasher class API imports
var FileNameMatcher = FileNameMatcherClass.static;
var HashedDir = HashedDirClass.static;
var HashedFile = HashedFileClass.static;
var HashedEntryType = HashedEntryTypeClass.static;

// Serialization class API imports
var HInput = HInputClass.static;
var HOutput = HOutputClass.static;
var StreamObject = StreamObjectClass.static;
var StreamObjectAdapter = StreamObjectAdapterClass.static;
var SignedBytesHolder = SignedBytesHolderClass.static;
var SignedObjectHolder = SignedObjectHolderClass.static;
var EnumSerializer = EnumSerializerClass.static;

// Config serialization class bindings
var ConfigObject = ConfigObjectClass.static;
var ConfigObjectAdapter = ConfigObjectAdapterClass.static;
var BlockConfigEntry = BlockConfigEntryClass.static;
var BooleanConfigEntry = BooleanConfigEntryClass.static;
var IntegerConfigEntry = IntegerConfigEntryClass.static;
var ListConfigEntry = ListConfigEntryClass.static;
var StringConfigEntry = StringConfigEntryClass.static;
var ConfigEntryType = ConfigEntryTypeClass.static;
var TextConfigReader = TextConfigReaderClass.static;
var TextConfigWriter = TextConfigWriterClass.static;

// Helper class API imports
var CommonHelper = CommonHelperClass.static;
var IOHelper = IOHelperClass.static;
var JVMHelper = JVMHelperClass.static;
var JVMHelperOS = JVMHelperOSClass.static;
var LogHelper = LogHelperClass.static;
var EnvHelper = EnvHelperClass.static;
var SecurityHelper = SecurityHelperClass.static;
var DigestAlgorithm = DigestAlgorithmClass.static;
var VerifyHelper = VerifyHelperClass.static;
var RingProgressIndicator = RingProgressIndicatorClass.static
var RingProgressIndicatorSkin = RingProgressIndicatorSkinClass.static

// Helper JS class API imports
var JSApplication = null;
if (typeof JSApplicationClass !== 'undefined') {
    JSApplication = JSApplicationClass.static;
}

// API wrapper
function tryWithResources(closeable, f) {
    try {
        f(closeable);
    } finally {
        IOHelper.close(closeable);
    }
}

function newTask(r) {
    return new javafx.concurrent.Task() { call: r };
}

function newRequestTask(request) {
    return newTask(function() request.request());
}

function startTask(task) {
    CommonHelper.newThread("FX Task Thread", true, task).start();
}

function openURL(url) {
    app.getHostServices().showDocument(url.toURI());
}

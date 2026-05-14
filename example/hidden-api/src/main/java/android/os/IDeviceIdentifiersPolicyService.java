package android.os;

public interface IDeviceIdentifiersPolicyService extends IInterface {
    String getSerial();

    abstract class Stub extends Binder implements IDeviceIdentifiersPolicyService {

        public static IDeviceIdentifiersPolicyService asInterface(IBinder ignoredObj) {
            throw new RuntimeException();
        }
    }
}

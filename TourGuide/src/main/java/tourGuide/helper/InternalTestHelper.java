package tourGuide.helper;

/**
 * InternalTestHelper is used to allow dummy users to be created for testing purposes
 *
 */
public class InternalTestHelper {

	// Set this default up to 100,000 for testing
	private static int internalUserNumber = 100;

	/**
	 * Method to update number of users to be created
	 *
	 * @param internalUserNumber number of users to be created
	 */
	public static void setInternalUserNumber(int internalUserNumber) {
		InternalTestHelper.internalUserNumber = internalUserNumber;
	}

	/**
	 * Get method for number of users to be created
	 *
	 * @return current value of internalUserNumber
	 */
	public static int getInternalUserNumber() {
		return internalUserNumber;
	}
}

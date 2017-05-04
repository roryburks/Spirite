package spirite.base.graphics.gl.wrap;

public abstract class GLCore {
	public static class MGLException extends Exception {
		public MGLException(String message) {
			super(message);
		}
	}
	
	private int shaderVersion = 0;
	
	public int getShaderVersion() {
		return shaderVersion;
	}
	public void setShaderVersion( int version) {
		shaderVersion = version;
	}
	
	public abstract boolean supportsGeometryShader();
	
}
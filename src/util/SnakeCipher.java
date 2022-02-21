package inertiax.util;

public class SnakeCipher
{
	private final byte[] _keys;
	
	public SnakeCipher(byte ... keys)
	{
		_keys = keys;
	}
	
	public byte[] encdec(byte[] in) 
	{
		final byte[] out = new byte[in.length];
		for (int i=0;i<in.length;i++)
		{
			final byte key = _keys[i % _keys.length];
			out[i] = (byte) (in[i] ^ key);
		}
		return out;
	}
	
	public void mencdec(byte[] in) 
	{
		for (int i=0;i<in.length;i++)
			in[i] = (byte) (in[i] ^ (_keys[i % _keys.length]));
	}
	
	public byte[] sencdec(byte[] in, int offset)
	{
		final byte[] out = new byte[in.length];
		for (int i=0;i<in.length;i++)
		{
			final int indx = (i + offset) % _keys.length;
			byte key = _keys[indx];
			out[i] = (byte) (in[i] ^ key);
		}
		return out;
	}
	
	public byte[] encd(byte[] in) 
	{
		final byte[] out = new byte[in.length];
		for (int i=0;i<in.length;i++)
		{
			byte key = _keys[i % _keys.length];
			System.out.print(String.format("%02X XOR %02X ", in[i], key));
			out[i] = (byte) (in[i] ^ key);
			System.out.print(String.format("= %02X\n", out[i]));
		}
		return out;
	}
}

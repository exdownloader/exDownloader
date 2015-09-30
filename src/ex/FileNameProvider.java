package ex;

public class FileNameProvider
{
    private String _formatString = "%d";
    private int _currentIndex = 0;

    public FileNameProvider(int count)
    {
        int padding = String.valueOf(count).length();
        _formatString = "%0" + String.valueOf(padding) + "d";
    }
    public String getNextName()
    {
        return String.format(_formatString, ++_currentIndex);
    }
}

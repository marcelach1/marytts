/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.util.data.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import marytts.server.MaryProperties;
import marytts.signalproc.display.FunctionGraph;
import marytts.util.MaryUtils;



public class AudioDestination
{
    /**
     * The mary property setting determining where to save audio data.
     */
    private static final String audiostoreProperty = MaryProperties.getProperty("synthesis.audiostore", "ram");

    private OutputStream os;
    private File f;
    private boolean ram;
     
    /**
     * Create an AudioDestination to which the audio data can be written.
     * Depending on the mary property "synthesis.audiostore",
     * this will use either a ByteArrayOutputStream or a FileOutputStream.
     * The calling code is responsible for administering this AudioDestination. 
     * @throws IOException if the underlying OutputStream could not be created. 
     */
    public AudioDestination() throws IOException
    {
        if (audiostoreProperty.equals("ram")) ram = true;
        else if (audiostoreProperty.equals("file")) ram = false;
        else // auto
            if (MaryUtils.lowMemoryCondition()) ram = false;
            else ram = true;
        if (ram) {
            os = new ByteArrayOutputStream();
            f = null;
        } else {
            f = MaryUtils.createSelfDeletingTempFile(3600);
            os = new FileOutputStream(f);
        }
    }

    public boolean isInRam() { return ram; }
    public boolean isFile() { return !ram; }

    public void write(byte[] b) throws IOException
    {
        os.write(b);
    }
    
    public void write(byte[] b, int off, int len) throws IOException
    {
        os.write(b, off, len);
    }

    /**
     * Convert the audio data into an AudioInputStream
     * of the proper AudioFormat.
     * @param audioFormat the format of the audio data.
     * @return an AudioInputStream from which the synthesised audio data can be
     * read.
     * @throws IOException if a problem occurred with the temporary file (only applies
     * when using files as temporary storage).
     */
    public AudioInputStream convertToAudioInputStream(AudioFormat audioFormat)
    throws IOException
    {
        if (ram) {
            assert os instanceof ByteArrayOutputStream;
            assert f == null;
            byte[] audioData = ((ByteArrayOutputStream)os).toByteArray();
            //logger.debug("Total of " + audioData.length + " bytes of audio data for this section.");
            return new AudioInputStream(
                new ByteArrayInputStream(audioData),
                audioFormat,
                audioData.length / audioFormat.getFrameSize());
        } else {
            assert os instanceof FileOutputStream;
            assert f != null; 
            os.close();
            long byteLength = f.length();
            return new AudioInputStream(
                new FileInputStream(f),
                audioFormat,
                byteLength / audioFormat.getFrameSize());
        }
    }        
    /**
     * Convert the audio data into an AudioInputStream
     * of the proper AudioFormat. This method assumes that the audio data
     * starts with a valid audio file header, so the audio format is read
     * from the data.
     * @return an AudioInputStream from which the synthesised audio data can be
     * read.
     * @throws IOException if a problem occurred with the temporary file (only applies
     * when using files as temporary storage).
     */
    public AudioInputStream convertToAudioInputStream()
    throws IOException, UnsupportedAudioFileException
    {
        if (ram) {
            assert os instanceof ByteArrayOutputStream;
            assert f == null;
            byte[] audioData = ((ByteArrayOutputStream)os).toByteArray();
            //logger.debug("Total of " + audioData.length + " bytes of audio data for this section.");
            return AudioSystem.getAudioInputStream(
                new ByteArrayInputStream(audioData));
        } else {
            assert os instanceof FileOutputStream;
            assert f != null; 
            os.close();
            long byteLength = f.length();
            return AudioSystem.getAudioInputStream(f);
        }
    }        

    public static void plot(double [] x)
    {
        plot(x, false);
    }
    
    public static void plot(double [] x, boolean bAutoClose)
    {
        plot(x, bAutoClose, 3000);
    }
    
    // Plots the values in x
    // If bAutoClose is specified, the figure is closed after milliSecondsToClose milliseconds
    // milliSecondsToClose: has no effect if bAutoClose is false
    public static void plot(double [] x, boolean bAutoClose, int milliSecondsToClose)
    {
        FunctionGraph graph = new FunctionGraph(400, 200, 0, 1, x);
        JFrame frame = graph.showInJFrame("wgt2", 500, 300, true, false);
        
        if (bAutoClose)
        {
            try { Thread.sleep(milliSecondsToClose); } catch (InterruptedException e) {}
            frame.dispose();
        }
    }
}
/* This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

//ButtonFactory.java - contains convenience methods to create buttons
//(C)2000-2001 Mark Beynon
//mark@jpedit.co.uk
//http://www.jpedit.co.uk
package com.utils;

import java.awt.*;
import javax.swing.*;
import com.jpe.jpe;
import com.jpe.Constants;

public class ButtonFactory implements Constants {

	public static JButton createButton(String iconFilename, String tooltip, jpe win) {
		JButton button = new JButton(new ImageIcon("images"+fileSep + iconFilename, tooltip));
		button.setFont(SYSTEM_FONT);
//		button.setToolTipText(tooltip);
		button.setPreferredSize(new Dimension(23, 23));
		button.addActionListener(win);
		button.setBorderPainted(false);
		button.addMouseListener(win);

		return button;
	}

	public static JButton createButton(String iconFilename, String tooltip, String s, jpe win) {
		JButton button = new JButton(new ImageIcon("images"+fileSep + iconFilename, tooltip));
		button.setFont(SYSTEM_FONT);
		button.setPreferredSize(new Dimension(23, 23));
		button.setBorderPainted(false);
		button.addMouseListener(win);
		if (s.equals("D")) {
			button.setEnabled(false);
		}
		return button;
	}

	public static JButton createButton(String iconFilename, int w, int h, jpe win) {
		JButton button = new JButton(new ImageIcon("images"+fileSep + iconFilename));
		button.setFont(SYSTEM_FONT);
		button.setRequestFocusEnabled(false);
		button.setBorderPainted(false);
		button.setPreferredSize(new Dimension(w, h));
		return button;
	}

}

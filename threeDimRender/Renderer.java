package threeDimRender;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Renderer extends JPanel implements KeyListener {

	private static final long serialVersionUID = 1L;
	
	static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	double[] frameDimensions = {screenSize.getWidth(), screenSize.getHeight()};
	
	//Static Type Screen Dims
	static double width = screenSize.getWidth();
	static double height = screenSize.getHeight();
	
	static Renderer cr = new Renderer();
	
	//Center of screen
	int centerX = (int) frameDimensions[0] / 2;
	int centerY = (int) frameDimensions[1] / 2;
	
	//Camera FOV Vector
	double[] camFovVec = new double[3];
	
	//Projection Bivector Stuff
	double[] horiProjVec = new double[3];
	double horiProjVecLen = 0.0;
	double[] vertProjVec = new double[3];
	double vertProjVecLen = 0.0;
	double[] projBiVec = new double[3];
	
	double aspectRatio = frameDimensions[0] / frameDimensions[1];
	
	//Constructor
	public Renderer() {
		
		//Default Background
		this.setBackground(Color.black);
		
		//Camera FOV Vector Intialization + Setup
		camFovVec[2] = 1.0;
		
		double[] rotVec1 = new double[3];
		double[] rotVec2 = new double[3];
		double[] rotVec3 = new double[3];
		
		rotVec1[0] = 1.0;
		
		//90 deg FOV
		rotVec2[0] = Math.cos(Math.PI / 8.0);
		rotVec2[2] = Math.sin(Math.PI / 8.0);
		
		camFovVec = rotateVec3D(camFovVec, rotVec1, rotVec2);
		
		//Setup Horizontal Component of Projection Bivector 
		horiProjVecLen = 2 * Math.abs(camFovVec[0]);
		horiProjVec[0] = 1.0; //Only capture direction to ease future calculations
		
		//Setup Vertical Component of Projection Bivector to respect Aspect Ratio Of Display
		vertProjVecLen = horiProjVecLen / aspectRatio;
		vertProjVec[1] = -1.0;
		
		//Get Angle to rotate CamFovVec to get proper vertical component as decided above
		double liftAngle = Math.asin((vertProjVecLen / 2.0) / (camFovVec[2]));
		
		rotVec1[0] = 0.0;
		rotVec1[2] = 1.0;
		
		rotVec3[2] = Math.cos(liftAngle / 2.0);
		rotVec3[1] = Math.sin(liftAngle / 2.0);
		
		camFovVec = rotateVec3D(camFovVec, rotVec1, rotVec3); 
		//Note: Rotation Plane is purposely not camFovVec above 
		//to ensure horizontal fov remains constant
		
		//Projection Bivector
		projBiVec = biVecCompMul3D(horiProjVec, vertProjVec);
		
		//Temp Testing Triangle Init:
		
		triangles[0][0][0] = -5.0;
		triangles[0][0][1] = -5.0;
		triangles[0][0][2] = 5.0;
		
		triangles[0][1][0] = 0.0;
		triangles[0][1][1] = 10.0;
		triangles[0][1][2] = 0.0;
		
		triangles[0][2][0] = 25.0;
		triangles[0][2][1] = 25.0;
		triangles[0][2][2] = 25.0;
		
		
		triangles[1][0][0] = triangles[0][0][1];
		triangles[1][0][1] = 5.0;
		triangles[1][0][2] = 5.0;
		
		triangles[1][1][0] = triangles[0][1][1];
		triangles[1][1][1] = 10.0;
		triangles[1][1][2] = 0.0;
		
		triangles[1][2][0] = 25.0;
		triangles[1][2][1] = triangles[0][2][1];
		triangles[1][2][2] = 25.0;
		
	}
	
	public static void main(String[] args) {
		
		JFrame frame = new JFrame("Testing");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize((int) width, (int) height);
		frame.setLocationRelativeTo(null);
		frame.setUndecorated(true);
		
		frame.add(cr);
		frame.addKeyListener(cr);
		
		frame.setVisible(true);
		
	}
	
	//Triangle List: TODO Make ArrayList
	double[][][] triangles = new double[2][3][3]; //[[x0,x1,x2],[y0,y1,y2],[z0,z1,z2]]
	
	long repaintCnt = 0;
	long t = System.currentTimeMillis();
	
	//Screen Painter
	public void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		
		g.setColor(Color.yellow);
		
		//3D Space -> 2D Screen Projection Per Triangle:
		for(double[][] oVec : triangles) { //TODO Parallel Computing Needed Here
			
			//Get scaler lambda's numerator value, to scale down originTo3DPoint vector
			//onto screen's projection bivector/plane
			double lambdaNumer = camFovVec[0] * projBiVec[1];
			lambdaNumer -= camFovVec[1] * projBiVec[2];
			lambdaNumer += camFovVec[2] * projBiVec[0];
			
			//Will Store Position On Screen
			int[] posX = new int[3];
			int[] posY = new int[3];
			
			//Triangle has three verticies i.e. points
			//here in format: [(x1,x2,x3),(y1,y2,y3),(z1,z2,z3)]
			//e.g. (x1,y1,z1) all refer to point 1
			//Iterate through these 3 points
			for(int i = 0; i < 3; ++i) {
				
				//Calculate Lambda
				double lambda = oVec[0][i] * projBiVec[1];
				lambda -= oVec[1][i] * projBiVec[2];
				lambda += oVec[2][i] * projBiVec[0];
				
				lambda = lambdaNumer / lambda;
				
				//Frame Vector, i.e. originTo3DPoint wrt position of Proj Bivector
				double[] fVec = new double[3];
				
				fVec[0] = (lambda * oVec[0][i]) - camFovVec[0];
				fVec[1] = (lambda * oVec[1][i]) - camFovVec[1];
				fVec[2] = (lambda * oVec[2][i]) - camFovVec[2];
				
				//Get "at after what % of the screen does this point exist" for x & y axes
				double hProj = dotProd3D(fVec, horiProjVec);
				hProj /= horiProjVecLen;
				
				double vProj = dotProd3D(fVec, vertProjVec);
				vProj /= vertProjVecLen; //To get %
				
				//Based on the %, get actual integer value position
				posX[i] = (int) (hProj * width);
				posY[i] = (int) (vProj * height);
				
			}
			
			//Finally, draw/fill these shapes
			g.fillPolygon(posX, posY, 3);
			
		}
		
		//Temp Stuff To Test 3D effect:-
		rad += 0.004;
				
		triangles[0][1][1] = 10.0 * Math.cos(rad);
		triangles[0][2][1] = 25.0 + 5.0 * Math.sin(rad);
				
		triangles[1][1][0] = triangles[0][1][1];
		triangles[1][1][1] = triangles[0][1][1];;
				
		triangles[1][2][0] = triangles[0][2][1];
		triangles[1][2][1] = triangles[0][2][1];
		
		//Repaint/Refresh Rate Counter
		if(System.currentTimeMillis() - t >= 1000) {
			System.out.println(repaintCnt);
			repaintCnt = 0;
			t = System.currentTimeMillis();
		}
		
		++repaintCnt;
		
		repaint();
		
	}
	
	//Reflect A through B (3D 1-vectors)
	//Rotation Formula : cbabc
	public double[] reflectVec3D(double[] a, double[] b) {
		
		//Geometric Algebra FTW :
		
		double Aa = a[0] * b[0];
		double Ab = a[0] * b[1];
		double Ac = a[0] * b[2];
		
		double Ba = a[1] * b[0];
		double Bb = a[1] * b[1];
		double Bc = a[1] * b[2];
		
		double Ca = a[2] * b[0];
		double Cb = a[2] * b[1];
		double Cc = a[2] * b[2];
		
		double[] newVec = new double[3];
		
		newVec[0] = Aa * b[0];
		newVec[0] -= Ab * b[1];
		newVec[0] -= Ac * b[2];
		newVec[0] += 2 * Ba * b[1];
		newVec[0] += 2 * Ca * b[2];
		
		newVec[1] = Bb * b[1];
		newVec[1] -= Ba * b[0];
		newVec[1] -= Bc * b[2];
		newVec[1] += 2 * Cb * b[2];
		newVec[1] += 2 * Aa * b[1];
		
		newVec[2] = Cc * b[2];
		newVec[2] -= Ca * b[0];
		newVec[2] -= Cb * b[1];
		newVec[2] += 2 * Aa * b[2];
		newVec[2] += 2 * Bb * b[2];
		
		return newVec;
	}
	
	//Rotate in direction of b to c
	public double[] rotateVec3D(double[] vecToRot, double[] b, double[] c) {
		
		double[] newVec = new double[3];
		
		newVec = reflectVec3D(vecToRot, b);
		newVec = reflectVec3D(newVec, c);
		
		return newVec;
	}
	
	//Bivector component of multiplication of two 3D 1-vectors
	public double[] biVecCompMul3D(double[] a, double[] b) {
		
		double[] result = new double[3];
		
		result[0] = a[0] * b[1];
		result[0] -= a[1] * b[0];
		
		result[1] = a[1] * b[2];
		result[1] -= a[2] * b[1];
		
		result[2] = a[0] * b[2];
		result[2] -= a[2] * b[0];
		
		return result; //xy, yz, xz
	}
	
	//return dot product of two 3D 1-vectors
	public double dotProd3D(double[] a, double[] b) {
		
		double val = a[0] * b[0];
		val += a[1] * b[1];
		val += a[2] * b[2];
		
		return val;
	}
	
	@Override
	public void keyTyped(KeyEvent e) {}
	
	double rad = 0.0;
	
	@Override
	public void keyPressed(KeyEvent e) {
		
		if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
			System.exit(0);
		
		
		if(e.getKeyCode() == KeyEvent.VK_1) {
			
			rad += 0.1;
			
			triangles[0][1][1] = 5.0 * Math.cos(rad);
			triangles[0][2][1] = 25.0 + 5.0 * Math.sin(rad);
			
			triangles[1][1][0] = triangles[0][1][1];
			triangles[1][1][1] = triangles[0][1][1];;
			
			triangles[1][2][0] = triangles[0][2][1];
			triangles[1][2][1] = triangles[0][2][1];
			
		}
		
		if(e.getKeyCode() == KeyEvent.VK_2) {}
		
	}

	@Override
	public void keyReleased(KeyEvent e) {}
	
}

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
	
	double fov = Math.PI / 2.0;
	
	//Camera Vectors
	double[] camTL = new double[3];
	double[] camBL = new double[3];
	double[] camTR = new double[3];
	
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
		camTL[2] = 1.0;
		camTR[2] = 1.0;
		camBL[2] = 1.0;
		
		double[] rotVec1 = new double[3];
		double[] rotVec2 = new double[3];
		double[] rotVec3 = new double[3];
		
		rotVec1[0] = 1.0;
		
		//FOV
		rotVec2[0] = Math.cos(fov / 4.0);
		rotVec2[2] = Math.sin(fov / 4.0);
		
		camTL = rotateVec3D(camTL, rotVec1, rotVec2);
		camBL = rotateVec3D(camBL, rotVec1, rotVec2);
		
		camTR = rotateVec3D(camTR, rotVec2, rotVec1); //Rotate in opposite direction (Right)
		
		//Setup Horizontal Component of Projection Bivector 
		horiProjVecLen = 2 * Math.abs(camTL[0]);
		horiProjVec[0] = 1.0; //Only capture direction to ease future calculations
		
		//Setup Vertical Component of Projection Bivector to respect Aspect Ratio Of Display
		vertProjVecLen = horiProjVecLen / aspectRatio;
		vertProjVec[1] = -1.0;
		
		if(vertProjVecLen / 2.0 > camTL[0]) {
			System.err.println("vertProjVecLen is unreasonably large! Change FOV!");
			System.exit(0);
		} //TODO Explore this more
		
		//Get Angle to rotate CamFovVec to get proper vertical component as decided above
		double liftAngle = Math.asin((vertProjVecLen / 2.0) / camTL[0]);
		//Dividing by camTL[0] as it is the radius (hypotenuse) of the slice of circle in the sphere
		//that these vectors are located in
		
		rotVec1[0] = 0.0;
		rotVec1[2] = 1.0;
		
		rotVec3[2] = Math.cos(liftAngle / 2.0);
		rotVec3[1] = Math.sin(liftAngle / 2.0);
		
		camTL = rotateVec3D(camTL, rotVec1, rotVec3);
		camTR = rotateVec3D(camTR, rotVec1, rotVec3);
		
		camBL = rotateVec3D(camBL, rotVec3, rotVec1); //Rotate in opposite direction (Down)
		//Note: Rotation Plane is purposely not camFovVec above 
		//to ensure horizontal fov remains constant
		
		//Projection Bivector
		horiProjVec = sub3D(camTR, camTL);
		vertProjVec = sub3D(camBL, camTL);
		System.out.println(horiProjVec[0] + " " + vertProjVec[1]);
		horiProjVec = normalize3D(horiProjVec);
		vertProjVec = normalize3D(vertProjVec);
		projBiVec = biVecCompMul3D(horiProjVec, vertProjVec);
		
		//Temp Testing Triangle Init:
		
		triangles[0][0][0] = -5.0;
		triangles[0][0][1] = -5.0;
		triangles[0][0][2] = 5.0;
		
		triangles[0][1][0] = 0.0;
		triangles[0][1][1] = 10.0;
		triangles[0][1][2] = 0.0;
		
		triangles[0][2][0] = 50.0;
		triangles[0][2][1] = 50.0;
		triangles[0][2][2] = 50.0;
		
		
		triangles[1][0][0] = triangles[0][0][1];
		triangles[1][0][1] = 5.0;
		triangles[1][0][2] = 5.0;
		
		triangles[1][1][0] = triangles[0][1][1];
		triangles[1][1][1] = 10.0;
		triangles[1][1][2] = 0.0;
		
		triangles[1][2][0] = 50.0;
		triangles[1][2][1] = triangles[0][2][1];
		triangles[1][2][2] = 50.0;
		
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
			double lambdaNumer = camTL[0] * projBiVec[1];
			lambdaNumer -= camTL[1] * projBiVec[2];
			lambdaNumer += camTL[2] * projBiVec[0];
			
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
				
				fVec[0] = (lambda * oVec[0][i]) - camTL[0];
				fVec[1] = (lambda * oVec[1][i]) - camTL[1];
				fVec[2] = (lambda * oVec[2][i]) - camTL[2];
				
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
			g.drawPolygon(posX, posY, 3);
			
		}
		
		//Temp Stuff To Test 3D effect:-
		rad += 0.004;
				
		triangles[0][1][1] = 10.0 * Math.cos(rad);
		triangles[0][2][1] = 50.0 + 10.0 * Math.sin(rad);
				
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
	
	public double[] add3D(double[] a, double[] b) {
		double[] ret = new double[3];
		
		ret[0] = a[0] + b[0];
		ret[1] = a[1] + b[1];
		ret[2] = a[2] + b[2];
		
		return ret;
	}
	
	public double[] sub3D(double[] a, double[] b) {
		double[] ret = new double[3];
		
		ret[0] = a[0] - b[0];
		ret[1] = a[1] - b[1];
		ret[2] = a[2] - b[2];
		
		return ret;
	}
	
	public double[] normalize3D(double[] vec) {
		
		double[] normed = new double[3];
		
		double x = Math.sqrt(vec[0]*vec[0] + vec[1]*vec[1] + vec[2]*vec[2]);
		
		normed[0] = vec[0] / x;
		normed[1] = vec[1] / x;
		normed[2] = vec[2] / x;
		
		return normed;
	}
	
	@Override
	public void keyTyped(KeyEvent e) {}
	
	double rad = 0.0;
	double[] camPos = new double[3];
	
	double[] rotV0 = {1.0, 0.0, 0.0};
	double[] rotV1 = {Math.cos(Math.PI / 180.0), 0.0, Math.sin(Math.PI / 180.0)};
	
	double[] rotV0b = {1.0, 0.0, 0.0};
	double[] rotV1b = {Math.cos(Math.PI / 180.0), 0.0, Math.sin(Math.PI / 180.0)};
	
	double[] rotV2 = {0.0, 0.0, 1.0};
	double[] rotV3 = {0.0, Math.sin(Math.PI / 180.0), Math.cos(Math.PI / 180.0)};
	
	@Override
	public void keyPressed(KeyEvent e) {
		
		if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
			System.exit(0);
		
		
		/*if(e.getKeyCode() == KeyEvent.VK_1) {
			
			rad += 0.1;
			
			triangles[0][1][1] = 10.0 * Math.cos(rad);
			triangles[0][2][1] = 50.0 + 10.0 * Math.sin(rad);
			
			triangles[1][1][0] = triangles[0][1][1];
			triangles[1][1][1] = triangles[0][1][1];;
			
			triangles[1][2][0] = triangles[0][2][1];
			triangles[1][2][1] = triangles[0][2][1];
			
		}
		
		if(e.getKeyCode() == KeyEvent.VK_LEFT) {
			
			camFovVec = rotateVec3D(camFovVec, rotV0, rotV1);
			horiProjVec = rotateVec3D(horiProjVec, rotV0b, rotV1b);
			projBiVec = biVecCompMul3D(horiProjVec, vertProjVec);
			rotV2 = rotateVec3D(rotV2, rotV0, rotV1);
			rotV3 = rotateVec3D(rotV3, rotV0, rotV1);
			
		}
		
		if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
			
			camFovVec = rotateVec3D(camFovVec, rotV1, rotV0);
			horiProjVec = rotateVec3D(horiProjVec, rotV1b, rotV0b);
			projBiVec = biVecCompMul3D(horiProjVec, vertProjVec);
			rotV2 = rotateVec3D(rotV2, rotV1, rotV0);
			rotV3 = rotateVec3D(rotV3, rotV1, rotV0);
			
		}*/
		
		if(e.getKeyCode() == KeyEvent.VK_UP) {
			
//			camFovVec = rotateVec3D(camFovVec, rotV2, rotV3);
//			vertProjVec = rotateVec3D(vertProjVec, rotV2, rotV3);
//			projBiVec = biVecCompMul3D(horiProjVec, vertProjVec);
/*			rotV0b = rotateVec3D(rotV0b, rotV2, rotV3);
			rotV1b = rotateVec3D(rotV1b, rotV2, rotV3);
*/			
		}
		
		if(e.getKeyCode() == KeyEvent.VK_DOWN) {
			
//			camFovVec = rotateVec3D(camFovVec, rotV3, rotV2);
//			vertProjVec = rotateVec3D(vertProjVec, rotV3, rotV2);
//			projBiVec = biVecCompMul3D(horiProjVec, vertProjVec);
/*			rotV0b = rotateVec3D(rotV0b, rotV3, rotV2);
			rotV1b = rotateVec3D(rotV1b, rotV3, rotV2);*/
			
		}
		
		if(e.getKeyCode() == KeyEvent.VK_W) {
			
		}
		
	}

	@Override
	public void keyReleased(KeyEvent e) {}
	
}

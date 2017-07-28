package de.metro.robocode;

import java.awt.Color;
import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.Condition;
import robocode.CustomEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class PizzaPower extends AdvancedRobot {
	private static final double WALL_MARGIN = 30;
	private static final int ROBOT_COUNT_FOR_WHICH_TO_CHANGE_STRATEGY = 3;
	private static final int MOVE_SIDEWAYS = 1;
	private static final int MOVE_CORNERS = 2;
	
	Enemy enemy = new Enemy();
	static double xForce;
	static double yForce;
	static int timeFromLastWallHit = 0;
	int moveStrategy = MOVE_CORNERS;
	int moveDirection = 1;
	
    @Override
    public void run() {
		setColors(Color.cyan, Color.cyan, Color.cyan);
		setBulletColor(Color.cyan);
		
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		
		addCustomEvent(new Condition("wallApproaching") {
			@Override
			public boolean test() {
				return getX() <= WALL_MARGIN || getX() >= getBattleFieldWidth() - WALL_MARGIN || getY() <= WALL_MARGIN
						|| getY() >= getBattleFieldHeight() - WALL_MARGIN;
			}
		});
		
        while (true) {
        	System.out.println("enemy = " + enemy + " others= " + getOthers());
        	System.out.println(timeFromLastWallHit);
        	
        	timeFromLastWallHit++;
        	
        	if (moveStrategy == MOVE_SIDEWAYS) {
				moveSideway();
				firePredictiveBullet();
			}
        	
        	setTurnRadarRight(360);
            
            execute();
            
			moveStrategy = getOthers() <= ROBOT_COUNT_FOR_WHICH_TO_CHANGE_STRATEGY ? MOVE_SIDEWAYS : MOVE_CORNERS;
        }
    }

	public void moveSideway() {
		setTurnRight(normalizeBearing(enemy.bearing + 90 - (15 * moveDirection)));

		if (getVelocity() == 0) {
			setMaxVelocity(8);
			
			moveDirection *= -1;
			
			setAhead(10000 * moveDirection);
		}
	}
	
    public void onScannedRobot(ScannedRobotEvent e) {
    	if (enemy.name == null || enemy.name.equals(e.getName()) || e.getDistance() < enemy.distance) {
    		enemy.update(e, getX(), getY(), getHeading());
    	}
    	
    	if (moveStrategy == MOVE_SIDEWAYS) {
    		return;
    	}
    	
    	double absoluteBearing =  e.getBearingRadians() + getHeadingRadians();
    	moveCorners(e, absoluteBearing);
    	
    	if (getGunHeat() == 0) {
			firePredictiveBullet();
    	}
    	
    	if (e.getName().equals(enemy.name)) {
    		lockRadarAndGunOnEnemy(absoluteBearing);
    	}
    }
    
	private void lockRadarAndGunOnEnemy(double absoluteBearing) {
		if (getGunHeat() < 1) {
			setTurnRadarLeft(getRadarTurnRemaining());
		}

		setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));
	}
	
    void firePredictiveBullet() {
    	if (enemy == null || enemy.name == null) {
    		return;
    	}
    	
    	double firePower = computeMinBulletPower(enemy.energy, enemy.distance);
		double bulletSpeed = 20 - firePower * 3;
		long time = (long) (enemy.distance / bulletSpeed);
		double futureX = enemy.getFutureX(time);
		double futureY = enemy.getFutureY(time);
		double absDeg = absoluteBearing(getX(), getY(), futureX, futureY);
		
    	setTurnGunRight(normalizeBearing(absDeg - getGunHeading()));
		
		if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
			System.out.println("setting fire = " + firePower);
			setFireBullet(firePower);
		}
    }
    
	double normalizeBearing(double angle) {
		while (angle > 180) {
			angle -= 360;
		}
		
		while (angle < -180) {
			angle += 360;
		}
		
		return angle;
	}
	
	double absoluteBearing(double x1, double y1, double x2, double y2) {
		double xo = x2 - x1;
		double yo = y2 - y1;
		double arcSin = Math.toDegrees(Math.asin(xo / Point2D.distance(x1, y1, x2, y2)));

		if (xo > 0 && yo > 0) { 
			return arcSin;
		} 
		
		if (xo < 0 && yo > 0) { 
			return 360.0 + arcSin;
		}						
		
		if (xo > 0 && yo < 0) { 
			return 180.0 - arcSin;
		}
			
		return 180.0 - arcSin;
	}

	private double computeMinBulletPower(double enemyEnergy, double distance) {
		if (enemyEnergy <= 4.0) {
			return Math.max(0.1, 0.00001 + enemyEnergy / 4.0);
		}
		
		if (enemyEnergy <= 16.0) {
			return Math.min(3.0, 0.00001 + (2.0 + enemyEnergy) / 6);
		}
		
		return Math.min(3.0, 400 / distance);
	}
	
    public void moveCorners(ScannedRobotEvent e, double absoluteBearing) {
		double distance = e.getDistance();

		xForce = xForce * .9 - Math.sin(absoluteBearing) / distance;
		yForce = yForce * .9 - Math.cos(absoluteBearing) / distance;

		setTurnRightRadians(
				Utils.normalRelativeAngle(Math.atan2(xForce + 1 / getX() - 1 / (getBattleFieldWidth() - getX()),
						yForce + 1 / getY() - 1 / (getBattleFieldHeight() - getY())) - getHeadingRadians()));
		
		setAhead(Double.POSITIVE_INFINITY);
		setMaxVelocity(420 / getTurnRemaining());
	}
    
    public void onCustomEvent(CustomEvent e) {
		if ("wallApproaching".equals(e.getCondition().getName())) {
			System.out.println("wall X= " + getX() + " Y= " + getY() + " timeFromLastWallHit= " + timeFromLastWallHit);
			
			if (timeFromLastWallHit > 100) {
				timeFromLastWallHit = 0;

				if (moveStrategy == MOVE_SIDEWAYS) {
					setMaxVelocity(0);
				}
			}
		}
	}
    
    @Override
    public void onHitWall(HitWallEvent event) {
    	System.out.println("wall hit " + event.getBearing() + " " + event.getTime());
    }

    public void onRobotDeath(RobotDeathEvent e) {
		if (e.getName().equals(enemy.name)) {
			enemy.name = null;
		}
	}
    
	public class Enemy {
		String name;
		double bearing;
		double distance;
		double energy;
		double heading;
		double velocity;
		private double x;
		private double y;

		public void update(ScannedRobotEvent e, double myX, double myY, double myHeading) {
			this.bearing = e.getBearing();
			this.distance = e.getDistance();
			this.energy = e.getEnergy();
			this.heading = e.getHeading();
			this.velocity = e.getVelocity();
			this.name = e.getName();
			

			double absBearingDeg = (myHeading + e.getBearing());
			if (absBearingDeg < 0) {
				absBearingDeg += 360;
			}

			this.x = myX + Math.sin(Math.toRadians(absBearingDeg)) * e.getDistance();
			this.y = myY + Math.cos(Math.toRadians(absBearingDeg)) * e.getDistance();
		}

		public double getFutureX(long when) {
			return x + Math.sin(Math.toRadians(heading)) * velocity * when;
		}

		public double getFutureY(long when) {
			return y + Math.cos(Math.toRadians(heading)) * velocity * when;
		}
		
		@Override
		public String toString() {
			return "Enemy [name=" + name + ", bearing=" + bearing + ", distance=" + distance + ", energy=" + energy
					+ ", heading=" + heading + ", velocity=" + velocity + ", x=" + x + ", y=" + y + "]";
		}
	}
}

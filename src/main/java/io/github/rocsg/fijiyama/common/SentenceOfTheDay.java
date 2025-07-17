/*
 * 
 */
package io.github.rocsg.fijiyama.common;

import java.util.Random;

import ij.IJ;
import io.github.rocsg.fijiyama.common.Timer;
import io.github.rocsg.fijiyama.common.VitimageUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class SentenceOfTheDay.
 */
public class SentenceOfTheDay {
	
	/**
	 * Gets the sentence of the day.
	 *
	 * @return the sentence of the day
	 */
	public static String getSentenceOfTheDay() {
		String[]sentences=VitimageUtils.readStringFromFile("src/main/resources/Zen_Quotes.txt").split("\n");
		int n=sentences.length-1;
		int val=new Timer().getAbsoluteDay();
		int chosen=1+val%n;
		return sentences[chosen];
	}
	
	/**
	 * Gets the random sentence.
	 *
	 * @return the random sentence
	 */
	public static String getRandomSentence() {
		String[]tab=getSentenceTab();
		Random rand=new Random();
		int line=rand.nextInt(tab.length-1);
		return ("#"+line+" "+tab[line]);
	}
	
	/**
	 * Gets the sentence tab.
	 *
	 * @return the sentence tab
	 */
	//#Yogi tea sentences, taken from http://www.debbyoga.com/debbyoga_relaunch/My_Sparks_files/Yogi%20Tea%20Bag%20Quotes.pdf
	public static String[]getSentenceTab() {
		return new String[] {
		"Cat",
		"Without the darkness, you would never know the light.",
		"There is no pain in the city of light.",
		"Act selfless, you will be infinite.",
		"Appreciate yourself and honor your soul.",
		"As a plant can’t live without roots, so a human can’t live without a soul.",
		"A relaxed mind is a creative mind.",
		"An attitude of gratitude brings you many opportunities.",
		"Act, don’t react. Always be pure, simple and honest.",
		"All knowledge is within you.",
		"Be proud of who you are.",
		"Bliss cannot be disturbed by gain or loss.",
		"By honoring your words, you are honored in this world.",
		"Be Happy so long as breath is in you.",
		"Be so happy that when others look at you they become happy too.",
		"Bliss is a constant state of mind, undisturbed by gain or loss.",
		"Be great, feel great and act great.",
		"By listening, you comfort another person.",
		"By kind and compassionate and the whole world will be your friend.",
		"Better to slip with your feet than with your tongue.",
		"Compassion has no limit. Kindness has no enemy.",
		"Chance multiply if you grab them.",
		"Delight the world with kindness, grace and compassion.",
		"Dignity and tranquility last forever.",
		"Don’t take pride in taking. Give and you will be given virtues.",
		"Delight the world with your compassion, kindness and grace.",
		"Don’t let yourself down, anyone else down or participate in a let down.",
		"Develop your intuition.",
		"Every heartbeat creates a miracle.",
		"Every smile is a direct achievement.",
		"Experience is wisdom.Experience the warmth and love of your soul.",
		"Empty yourself and let the universe fill you.",
		"Experience your own body, your own mind and your own soul.",
		"Every promise is a present in abundance.",
		"Every promise is a present in advancement.",
		"Feel great, act great and be great.",
		"Feel great, act great and approve of yourself.",
		"Find happiness within yourself. Then share yourself with others.",
		"For every loss there’s an equal gain, for every gain there’s an equal loss.",
		"Feel good, be good and do good.",
		"Grace brings contentment.",
		"Grace brings trust, appreciation, love and prosperity.",
		"Gratitude is the open door to abundance.",
		"Greatness is measured by your gifts, not your possessions.",
		"Goodness should become human nature, because it real in nature.",
		"Happiness comes from contentment.",
		"Happiness is every human being’s birthright.",
		"Have wisdom in your actions and faith in your merits.",
		"Happiness is nothing but total relaxation.",
		"Happiness comes when you overcome the most impossible challenge.",
		"Happiness is nothing but total relaxation.",
		"It’s not life that matters; it’s the courage that we bring to it.",
		"Inspiring others towards happiness brings you happiness.",
		"I am beautiful, I am bountiful, I am blissful.",
		"It is not what you have that is your greatness; it is what you can give.",
		"It’s important to find your identity and your legacy.",
		"In order to be remembered, leave nothing behind but goodness.",
		"If you don’t love where you come from, you can’t love where you are going.",
		"If you see good, learn something. If you see bad, learn what not to be.",
		"Inspiration is an unlimited power.",
		"Joy is the essence of success.",
		"Know whatever you are doing is the most beautiful thing.",
		"Keep up.",
		"Listen and you will develop intuition.",
		"Let Your Manners Speak for You.",
		"Let things come to you.",
		"Love, compassion and kindness are the anchors of life.",
		"Let your heart speak to others’ hearts.",
		"Love is where compassion prevails and kindness rules.",
		"Live from your heart and you will be most effective.",
		"Let your heart guide you. Love what is ahead by loving what has come before.",
		"Live in your strength.",
		"Live for each other.",
		"Love your soul.",
		"Live to share.",
		"Love is unchanging and limitless.",
		"Live with reverence for yourself and others.",
		"Life is a flow of love; your participation is requested.",
		"Life is a chance.",
		"Love is infinity. Grace is reality.",
		"Love has no fear and no vengeance.",
		"Love is ecstasy.",
		"Let Love elevate your self to excellence.",
		"Let People bask in your radiance and sunshine.",
		"Life is a gift. If you do not value your gift, nobody else will.",
		"Learn to be noble, courteous and committed.",
		"Live by intuition and consciousness.",
		"Live and let live.",
		"Let your mind dance with your body.",
		"Love is to live for each other.",
		"Man is as vast as he acts.",
		"May you inner self be secure and happy.",
		"Make yourself so happy that when others look at you they become happy too.",
		"Mental happiness is total relaxation.",
		"May this day bring you peace, tranquility and harmony.",
		"May you have faith in your worth and act with wisdom.",
		"May you have love, kindness and compassion for all living things.",
		"May your mind learn to love with compassion.",
		"Nature is a true giver, a true friend and a sustainer.",
		"Noble language and behaviors are so powerful that hearts can be melted.",
		"No snowflake ever falls in the wrong place.",
		"Our intuition lies in our innocence.",
		"Old age needs wisdom and grace.",
		"Open up to infinity and you become infinity.",
		"Oneness is achieved by recognizing your self.",
		"One of the best actions we can take with courage is to relax.",
		"Obey, serve, love and excel.",
		"Our thoughts are forming the world.",
		"Practice kindness, mercy and forgiveness.",
		"Practice kindness, compassion and caring.",
		"Provoke, confront, elevate.",
		"Patience gives the power to practice; practice gives the power that leads to perfection.",
		"Radiate the infinite light through your finite self.",
		"Recognize that the other person is you.",
		"Real happiness lies in that which never comes nor goes but simply is.",
		"Say it straight, simple and with a smile.",
		"Socialize with compassion and kindness.",
		"Speak the truth.",
		"Sing from your heart.",
		"Share your strengths, not your weaknesses.",
		"Self-reliance conquers any difficulty.",
		"Serve humanity so that people feel we are kind to them.",
		"Serve all without classification or discrimination",
		"Socialize with compassion, kindness and grace.",
		"Strength does not lie in what you have. It lies in what you can give.",
		"The moment you love, you are unlimited.",
		"The strength in you is your endurance.",
		"The intelligence in you is your vastness.",
		"The power of love is infinite.",
		"The universe is the stage on which you dance, guided by your heart.",
		"The art of happiness is to serve all.",
		"To be great, feel great and act great.",
		"The purpose of life is to enjoy every moment.",
		"To learn, read. To know, write. To master, teach.",
		"Truth is everlasting.",
		"To be calm is the highest achievement of the self.",
		"The beauty in you is your spirit.",
		"There is no greater power than the power of the word.",
		"The soul is projection: represent it.",
		"There is no greater power in this universe than the power of prayer.",
		"The mind is energy: regulate it.",
		"The Power of love is infinite.",
		"There is no love without compassion.",
		"The only tool you need is kindness.",
		"The body is a temple: take care of it.",
		"True understanding is found through compassion.",
		"The beauty of life is to experience yourself.",
		"The power of prayer extends happiness.",
		"There is beauty in your presence. Show who you are.",
		"Travel light, live light, spread the light, be the light.",
		"There are three values: Feel good, be good and do good.",
		"True wealth is the ability to let go of your possessions.",
		"To be healthy: eat right, walk right and talk to yourself right.",
		"True understanding is found through compassion.",
		"Those who live in the past limit their future.",
		"The trust that others place in you is your grace.",
		"The best way of life is to be, simply be.",
		"The greatest tool you have is to listen.",
		"The obstacle is the path.",
		"The rhythm of life is when you experience your own body, mind and soul.",
		"The universe is a stage on which your mind dances with your body, guided by your heart.",
		"Tranquility is the essence of life",
		"The art of longing and the art of belonging must be experienced in life.",
		"To know others is smart. To know yourself is wise.",
		"Trust creates peace.",
		"Trust the wisdom of the heart.",
		"The heart sees deeper than the eye.",
		"Uplift everybody and uplift yourself.",
		"Understanding is found through compassion.",
		"Unite with your own higher self and create a friendship.",
		"Walk as if you are kissing the Earth with your feet.",
		"When you know that all is light, you are enlightened.",
		"Where there is love, there is no question.",
		"When we practice listening, we become intuitive.",
		"Wisdom, character and consciousness conquer everything.",
		"Wisdom becomes knowledge when it is a personal experience.",
		"Whatever you are doing is the most beautiful thing.",
		"Without realizing who you are, happiness cannot come to you.",
		"We are spiritual beings having a human experience.",
		"When walking, walk. When eating, eat.",
		"When you reach the top of the mountain, keep climbing.When the mind is backed by will, miracles happen.",
		"Whatever character you give your children shall be their future.",
		"Whatever you are, you are. Be proud of it.",
		"We are here to love each other, serve each other and uplift each other.",
		"When you are in tune with the unknown, the known is peaceful.",
		"Work but don’t forget to live.",
		"You are unlimited.",
		"You are a living consciousness.",
		"You head must bow to your heart.",
		"Your intuition is your best friend.",
		"You are remembered for your goodness.",
		"Your word is your greatest power.",
		"Your destiny is to merge with infinity.",
		"Your heartbeat is the rhythm of your soul.",
		"Your greatest strength is love.",
		"Your infinity in you is the reality in you.",
		"Your breath is the voice of your soul.",
		"Your greatness is measured by your gifts, not by what you have.",
		"You must know that you can swim through every tide and change of time.",
		"You only give when you love.",
		"Your life is based on the capacity of energy in you not outside of you.",
		"You are infinite.",
		"You can run after satisfaction, but satisfaction must come from within.",
		"Your greatness is not what you have; it’s what you give.",
		"You will feel fulfilled when you do the impossible for someone else.",
		"Your soul is your highest self.",
		"Your strength is in how calmly, quietly and peacefully you face life.",
		"You must live for something higher, bigger and better than you."
		};

	}
}



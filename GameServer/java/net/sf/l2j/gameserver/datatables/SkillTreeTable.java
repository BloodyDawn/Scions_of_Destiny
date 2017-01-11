/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.datatables;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;

/**
 * This class ...
 * 
 * @version $Revision: 1.13.2.2.2.8 $ $Date: 2005/04/06 16:13:25 $
 */
public class SkillTreeTable
{
    private static Logger _log = Logger.getLogger(SkillTreeTable.class.getName());
    private static SkillTreeTable _instance;

    private Map<ClassId, Map<Integer,L2SkillLearn>> _skillTrees;
    private List<L2SkillLearn> _fishingSkillTrees; // all common skills (teached by Fisherman)
    private List<L2SkillLearn> _expandDwarfCraftSkillTrees; // list of special skill for dwarf (expand dwarf craft) learned by class teacher
    private Map<Integer, L2EnchantSkillLearn> _enchantSkillTrees; // enchant skill list

    // checker, sorted arrays of hashcodes
    private TIntObjectHashMap<int[]> _skillsByClassIdHashCodes; // occupation skills
    private int[] _allSkillsHashCodes; // fishing, and special

    private boolean _loading = true;

    public static SkillTreeTable getInstance()
    {
        if (_instance == null)
            _instance = new SkillTreeTable();

        return _instance;
    }

    /**
     * Return the minimum level needed to have this Expertise.<BR><BR>
     * 
     * @param grade The grade level searched
     */
    public int getExpertiseLevel(int grade) 
    {
        if (grade <= 0)
            return 0;

        // since expertise comes at same level for all classes we use paladin for now
        Map<Integer,L2SkillLearn> learnMap = getSkillTrees().get(ClassId.paladin);

        int skillHashCode = SkillTable.getSkillHashCode(239, grade);
        if (learnMap.containsKey(skillHashCode))
            return learnMap.get(skillHashCode).getMinLevel();

        _log.severe("Expertise not found for grade "+grade);
        return 0;
    }

    /**
     * Each class receives new skill on certain levels, this methods allow the retrieval of the minimun character level 
     * of given class required to learn a given skill
     * @param skillId The iD of the skill
     * @param classID The classId of the character
     * @param skillLvl The SkillLvl
     * @return The min level
     */
    public int getMinSkillLevel(int skillId, ClassId classId, int skillLvl) 
    {
        Map<Integer,L2SkillLearn> map = getSkillTrees().get(classId); 

        int skillHashCode = SkillTable.getSkillHashCode(skillId,skillLvl);

        if (map.containsKey(skillHashCode))
            return map.get(skillHashCode).getMinLevel();

        return 0;
    }

    public int getMinSkillLevel(int skillId, int skillLvl) 
    {
    	int skillHashCode = SkillTable.getSkillHashCode(skillId, skillLvl);

    	// Look on all classes for this skill (takes the first one found)
    	for (Map<Integer,L2SkillLearn> map : getSkillTrees().values())
    	{
            // checks if the current class has this skill
            if (map.containsKey(skillHashCode))
                return map.get(skillHashCode).getMinLevel();
    	}

        return 0;
    }

    private SkillTreeTable()
    {
        _loading = true;
        int classId = 0;
        int count = 0;

        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT * FROM class_list ORDER BY id");
            ResultSet classlist = statement.executeQuery())
        {
            Map<Integer, L2SkillLearn> map;
            int parentClassId;
            L2SkillLearn skillLearn;

            while (classlist.next())
            {
                map = new FastMap<>();
                parentClassId = classlist.getInt("parent_id");
                classId = classlist.getInt("id");

                if (parentClassId != -1)
                {
                    Map<Integer, L2SkillLearn> parentMap = getSkillTrees().get(ClassId.values()[parentClassId]);
                    map.putAll(parentMap);
                }

                try (PreparedStatement statement2 = con.prepareStatement("SELECT class_id, skill_id, level, name, sp, min_level FROM skill_trees where class_id=? ORDER BY skill_id, level"))
                {
                    statement2.setInt(1, classId);
                    try (ResultSet skilltree = statement2.executeQuery())
                    {
                        int prevSkillId = -1;

                        while (skilltree.next())
                        {
                            int id = skilltree.getInt("skill_id");
                            int lvl = skilltree.getInt("level");
                            String name = skilltree.getString("name");
                            int minLvl = skilltree.getInt("min_level");
                            int cost = skilltree.getInt("sp");

                            if (prevSkillId != id)
                                prevSkillId = id;

                            skillLearn = new L2SkillLearn(id, lvl, minLvl, name, cost, 0, 0);
                            map.put(SkillTable.getSkillHashCode(id,lvl), skillLearn);
                        }
                    }
                }
                getSkillTrees().put(ClassId.values()[classId], map);

                count += map.size();
                _log.fine("SkillTreeTable: skill tree for class " + classId + " has " + map.size() + " skills");		
            }
        }
        catch (Exception e)
        {
            _log.severe("Error while creating skill tree (Class ID " + classId + "):" + e);
        }

        _log.config("SkillTreeTable: Loaded " + count + " skills.");

        // Skill tree for fishing skill (from Fisherman)
        int count2   = 0;
        int count3   = 0;

        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT skill_id, level, name, sp, min_level, costid, cost, isfordwarf FROM fishing_skill_trees ORDER BY skill_id, level");
            ResultSet skilltree2 = statement.executeQuery())
        {
            _fishingSkillTrees = new FastList<>();
            _expandDwarfCraftSkillTrees = new FastList<>();

            int prevSkillId = -1;

            while (skilltree2.next())
            {
                int id = skilltree2.getInt("skill_id");
                int lvl = skilltree2.getInt("level");
                String name = skilltree2.getString("name");
                int minLvl = skilltree2.getInt("min_level");
                int cost = skilltree2.getInt("sp");
                int costId = skilltree2.getInt("costid");
                int costCount = skilltree2.getInt("cost");
                int isDwarven = skilltree2.getInt("isfordwarf");

                if (prevSkillId != id)
                    prevSkillId = id;

                L2SkillLearn skill = new L2SkillLearn(id, lvl, minLvl, name, cost, costId, costCount);

                if (isDwarven == 0)
                    _fishingSkillTrees.add(skill);
                else 
                    _expandDwarfCraftSkillTrees.add(skill);                    
            }            

            count2 = _fishingSkillTrees.size();
            count3 = _expandDwarfCraftSkillTrees.size();
        }
        catch (Exception e)
        {
            _log.severe("Error while creating fishing skill table: " + e);
        }

        int count4 = 0;

        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT skill_id, level, name, base_lvl, sp, min_skill_lvl, exp, success_rate76, success_rate77, success_rate78 FROM enchant_skill_trees ORDER BY skill_id, level");
            ResultSet skilltree3 = statement.executeQuery())
        {
            _enchantSkillTrees = new FastMap<>();

            int prevSkillId = -1;

            while (skilltree3.next())
            {
                int id = skilltree3.getInt("skill_id");
                int lvl = skilltree3.getInt("level");
                String name = skilltree3.getString("name");
                int baseLvl = skilltree3.getInt("base_lvl");
                int minSkillLvl = skilltree3.getInt("min_skill_lvl");
                int sp = skilltree3.getInt("sp");
                int exp = skilltree3.getInt("exp");
                byte rate76 = skilltree3.getByte("success_rate76");
                byte rate77 = skilltree3.getByte("success_rate77");
                byte rate78 = skilltree3.getByte("success_rate78");

                if (prevSkillId != id)
                    prevSkillId = id;

                L2EnchantSkillLearn skill = new L2EnchantSkillLearn(id, lvl, minSkillLvl, baseLvl, name, sp, exp, rate76 , rate77, rate78);

                _enchantSkillTrees.put(SkillTable.getSkillHashCode(id, lvl), skill);
            }

            count4 = _enchantSkillTrees.size();
        }
        catch (Exception e)
        {
            _log.severe("Error while creating enchant skill table: " + e);
        }

        generateCheckArrays();

        _log.config("FishingSkillTreeTable: Loaded " + count2 + " general skills.");
        _log.config("FishingSkillTreeTable: Loaded " + count3 + " dwarven skills.");
        _log.config("EnchantSkillTreeTable: Loaded " + count4 + " enchant skills.");
        _loading = false;
    }

    private void generateCheckArrays()
    {
        int i;
        int[] array;

        // class-specific skills
        Map<Integer, L2SkillLearn> tempMap;
        TIntObjectHashMap<int[]> result = new TIntObjectHashMap<int[]>(_skillTrees.keySet().size());
        for (ClassId cls : _skillTrees.keySet())
        {
            i = 0;
            tempMap = _skillTrees.get(cls);
            array = new int[tempMap.size()];

            for (int h : tempMap.keySet())
                array[i++] = h;

            Arrays.sort(array);
            result.put(cls.ordinal(), array);
        }
        _skillsByClassIdHashCodes = result;

        // skills available for all classes and races
        FastList<Integer> list = FastList.newInstance();

        for (L2SkillLearn s : _fishingSkillTrees)
            list.add(SkillTable.getSkillHashCode(s.getId(), s.getLevel()));

        for (L2SkillLearn s : _expandDwarfCraftSkillTrees)
            list.add(SkillTable.getSkillHashCode(s.getId(), s.getLevel()));

        i = 0;
        array = new int[list.size()];

        for (int s : list)
            array[i++] = s;

        Arrays.sort(array);
        _allSkillsHashCodes = array;

        FastList.recycle(list);
    }

    private Map<ClassId, Map<Integer, L2SkillLearn>> getSkillTrees()
    {
        if (_skillTrees == null)
            _skillTrees = new FastMap<>();

        return _skillTrees;
    }

    public L2SkillLearn[] getMaxAvailableSkills(L2PcInstance cha, ClassId classId)
    {
        Map<Integer, L2SkillLearn> result = new FastMap<>();
        Collection<L2SkillLearn> skills = getSkillTrees().get(classId).values();

        if (skills == null)
        {
            // the skilltree for this class is undefined, so we give an empty list
            _log.warning("Skilltree for class " + classId + " is not defined !");
            return new L2SkillLearn[0];
        }

        L2Skill[] oldSkills = cha.getAllSkills();

        for (L2SkillLearn temp : skills)
        {           
            if (temp.getMinLevel() <= cha.getLevel())
            {
                boolean knownSkill = false;

                for (int j = 0; j < oldSkills.length && !knownSkill; j++)
                {
                    if (oldSkills[j].getId() == temp.getId())
                    {
                        knownSkill = true;


                        if (oldSkills[j].getLevel() < temp.getLevel())
                        {
                            // this is the next level of a skill that we know
                            result.put(temp.getId(), temp);
                        }
                    }
                }


                if (!knownSkill)
                {
                    // this is a new skill
                    result.put(temp.getId(), temp);
                }
            }
        }

        return result.values().toArray(new L2SkillLearn[result.size()]);
    }

    public L2SkillLearn[] getAvailableSkills(L2PcInstance cha, ClassId classId)
    {
        List<L2SkillLearn> result = new FastList<>();
        Collection<L2SkillLearn> skills = getSkillTrees().get(classId).values();

        if (skills == null)
        {
            // the skilltree for this class is undefined, so we give an empty list
            _log.warning("Skilltree for class " + classId + " is not defined !");
            return new L2SkillLearn[0];
        }

        L2Skill[] oldSkills = cha.getAllSkills();

        for (L2SkillLearn temp : skills)
        {
            if (temp.getMinLevel() <= cha.getLevel())
            {
                boolean knownSkill = false;

                for (int j = 0; j < oldSkills.length && !knownSkill; j++)
                {
                    if (oldSkills[j].getId() == temp.getId())
                    {
                        knownSkill = true;

                        if (oldSkills[j].getLevel() == temp.getLevel()-1)
                        {
                            // this is the next level of a skill that we know
                            result.add(temp);
                        }
                    }
                }

                if (!knownSkill && temp.getLevel() == 1)
                {
                    // this is a new skill
                    result.add(temp);
                }
            }
        }

        return result.toArray(new L2SkillLearn[result.size()]);
    }

    public L2SkillLearn[] getAvailableSkills(L2PcInstance cha)
    {
        List<L2SkillLearn> result = new FastList<>();
	List<L2SkillLearn> skills = new FastList<>();

        skills.addAll(_fishingSkillTrees);

	if (skills == null)
	{
	    // the skilltree for this class is undefined, so we give an empty list
	    _log.warning("Skilltree for fishing is not defined !");
	    return new L2SkillLearn[0];
	}

        if (cha.hasDwarvenCraft() && _expandDwarfCraftSkillTrees != null)
            skills.addAll(_expandDwarfCraftSkillTrees);

        L2Skill[] oldSkills = cha.getAllSkills();

        for (L2SkillLearn temp : skills)
        {
            if (temp.getMinLevel() <= cha.getLevel())
            {
                boolean knownSkill = false;

                for (int j = 0; j < oldSkills.length && !knownSkill; j++)
                {
                    if (oldSkills[j].getId() == temp.getId())
                    {
                        knownSkill = true;

                        if (oldSkills[j].getLevel() == temp.getLevel()-1)
                        {
                            // this is the next level of a skill that we know
                            result.add(temp);
                        }
                    }
                }

                if (!knownSkill && temp.getLevel() == 1)
                {
                    // this is a new skill
                    result.add(temp);
                }
            }
        }

        return result.toArray(new L2SkillLearn[result.size()]);
    }

    public L2EnchantSkillLearn[] getAvailableEnchantSkills(L2PcInstance cha)
    {
        List<L2EnchantSkillLearn> result = new FastList<>();
        Map<Integer, L2EnchantSkillLearn> skills = new FastMap<>();

        skills.putAll(_enchantSkillTrees);

        if (skills == null)
        {
            // the skilltree for this class is undefined, so we give an empty list
            _log.warning("Skilltree for enchanting is not defined !");
            return new L2EnchantSkillLearn[0];
        }

        L2Skill[] oldSkills = cha.getAllSkills();

        for (L2EnchantSkillLearn temp : skills.values())
        {           
            if (cha.getLevel() >= 76)
            {
                boolean knownSkill = false;

                for (int j = 0; j < oldSkills.length && !knownSkill; j++)
                {
                    if (oldSkills[j].getId() == temp.getId())
                    {
                        knownSkill = true;

                        if ( oldSkills[j].getLevel() == temp.getMinSkillLevel())
                        {
                            // this is the next level of a skill that we know
                            result.add(temp);
                        }
                    }
                }
            }
        }

        return result.toArray(new L2EnchantSkillLearn[result.size()]);
    }

    /**
     * Returns all allowed skills for a given class.
     * @param classId
     * @return all allowed skills for a given class.
     */
    public Collection<L2SkillLearn> getAllowedSkills(ClassId classId)
    {
        return getSkillTrees().get(classId).values();
    }

    public int getMinLevelForNewSkill(L2PcInstance cha, ClassId classId)
    {
        int minLevel = 0;
        Collection<L2SkillLearn> skills = getSkillTrees().get(classId).values();

        if (skills == null)
        {
            // the skilltree for this class is undefined, so we give an empty list
            _log.warning("Skilltree for class " + classId + " is not defined !");
            return minLevel;
        }

        for (L2SkillLearn temp: skills)
        {           
            if (temp.getMinLevel() > cha.getLevel() && temp.getSpCost() != 0)
            {
                if (minLevel == 0 || temp.getMinLevel()<minLevel)
                    minLevel = temp.getMinLevel();
            }
        }

        return minLevel;
    }

    public int getMinLevelForNewSkill(L2PcInstance cha)
    {
        int minLevel = 0;       
        List<L2SkillLearn> skills = new FastList<>();

        skills.addAll(_fishingSkillTrees);

        if (skills == null)
        {
            // the skilltree for this class is undefined, so we give an empty list
            _log.warning("SkillTree for fishing is not defined!");
            return minLevel;
        }

        if (cha.hasDwarvenCraft() && _expandDwarfCraftSkillTrees != null)
            skills.addAll(_expandDwarfCraftSkillTrees);
        
        for (L2SkillLearn s : skills)
        {
            if (s.getMinLevel() > cha.getLevel())
            {
                if (minLevel == 0 || s.getMinLevel() < minLevel)
                    minLevel = s.getMinLevel();
            }
        }

        return minLevel;
    }

    public int getSkillCost(L2PcInstance player, L2Skill skill)
    {
        int skillCost = 100000000;
        ClassId classId = player.getSkillLearningClassId();
        int skillHashCode = SkillTable.getSkillHashCode(skill);

        if (getSkillTrees().get(classId).containsKey(skillHashCode))
        {
            L2SkillLearn skillLearn = getSkillTrees().get(classId).get(skillHashCode);
            if (skillLearn.getMinLevel() <= player.getLevel())
            {           
                skillCost = skillLearn.getSpCost();
                if (!player.getClassId().equalsOrChildOf(classId))
                {
                    if (skill.getCrossLearnAdd() < 0)
                        return skillCost;

                    skillCost += skill.getCrossLearnAdd();
                    skillCost *= skill.getCrossLearnMul();
                }

                if ((classId.getRace() != player.getRace()) && !player.isSubClassActive())
                    skillCost *= skill.getCrossLearnRace();

                if (classId.isMage() != player.getClassId().isMage())
                    skillCost *= skill.getCrossLearnProf();
            }
        }

        return skillCost;
    }

    public int getSkillSpCost(L2PcInstance player, L2Skill skill)
    {
        int skillCost = 100000000;
        L2EnchantSkillLearn[] enchantSkillLearnList = getAvailableEnchantSkills(player);

        for (L2EnchantSkillLearn enchantSkillLearn : enchantSkillLearnList)
        {
            if (enchantSkillLearn.getId() != skill.getId())
                continue;

            if (enchantSkillLearn.getLevel() != skill.getLevel())
                continue;

            if (player.getLevel() < 76)
                continue;

            skillCost = enchantSkillLearn.getSpCost();
        }

        return skillCost;
    }

    public int getSkillExpCost(L2PcInstance player, L2Skill skill)
    {
        int skillCost = 100000000;
        L2EnchantSkillLearn[] enchantSkillLearnList = getAvailableEnchantSkills(player);

        for (L2EnchantSkillLearn enchantSkillLearn : enchantSkillLearnList)
        {
            if (enchantSkillLearn.getId() != skill.getId())
                continue;

            if (enchantSkillLearn.getLevel() != skill.getLevel())
                continue;

            if (player.getLevel() < 76)
                continue;

            skillCost = enchantSkillLearn.getExp();
        }

        return skillCost;
    }

    public byte getSkillRate(L2PcInstance player, L2Skill skill)
    {
        L2EnchantSkillLearn[] enchantSkillLearnList = getAvailableEnchantSkills(player);

        for (L2EnchantSkillLearn enchantSkillLearn : enchantSkillLearnList)
        {
            if (enchantSkillLearn.getId() != skill.getId())
                continue;

            if (enchantSkillLearn.getLevel() != skill.getLevel())
                continue;

            return enchantSkillLearn.getRate(player);
        }
        return 0;
    }

    public boolean isSkillAllowed(L2PcInstance player, L2Skill skill)
    {
        if (player.isGM())
            return true;

        if (_loading) // prevent accidental skill remove during reload
            return true;

        int level = skill.getLevel();
        final int maxLvl = SkillTable.getInstance().getMaxLevel(skill.getId());
        int hashCode = SkillTable.getSkillHashCode(skill.getId(), level);

        if (_enchantSkillTrees.get(hashCode) != null)
            level = _enchantSkillTrees.get(hashCode).getBaseLevel();

        hashCode = SkillTable.getSkillHashCode(skill.getId(), Math.min(level, maxLvl));

        if (Arrays.binarySearch(_skillsByClassIdHashCodes.get(player.getClassId().ordinal()), hashCode) >= 0)
            return true;

        if (Arrays.binarySearch(_allSkillsHashCodes, hashCode) >= 0)
            return true;

        return false;
    }
}
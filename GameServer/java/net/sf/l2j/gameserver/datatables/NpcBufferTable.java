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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javolution.util.FastMap;

import net.sf.l2j.L2DatabaseFactory;

public class NpcBufferTable
{
    private static NpcBufferTable _instance = null;

    private Map<Integer, Integer> _skillId = new FastMap<>();
    private Map<Integer, Integer> _skillLevels = new FastMap<>();
    private Map<Integer, Integer> _skillFeeIds = new FastMap<>();
    private Map<Integer, Integer> _skillFeeAmounts = new FastMap<>();

    public void addSkill(int skillId, int skillLevel, int skillFeeId, int skillFeeAmount, int buffGroup)
    {
        _skillId.put(buffGroup, skillId);
        _skillLevels.put(buffGroup, skillLevel);
        _skillFeeIds.put(buffGroup, skillFeeId);
        _skillFeeAmounts.put(buffGroup, skillFeeAmount);
    }

    public int[] getSkillGroupInfo(int buffGroup)
    {
        Integer skillId = _skillId.get(buffGroup);
        Integer skillLevel = _skillLevels.get(buffGroup);
        Integer skillFeeId = _skillFeeIds.get(buffGroup);
        Integer skillFeeAmount = _skillFeeAmounts.get(buffGroup);

        if (skillId == null || skillLevel == null || skillFeeId == null || skillFeeAmount == null)
            return null;

        return new int[] {skillId, skillLevel, skillFeeId, skillFeeAmount};
    }

    private NpcBufferTable()
    {
        int skillCount = 0;

        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT skill_id, skill_level, skill_fee_id, skill_fee_amount, buff_group FROM npc_buffer order by id");
            ResultSet rset = statement.executeQuery())
        {
            while (rset.next())
            {
                int skillId = rset.getInt("skill_id");
                int skillLevel = rset.getInt("skill_level");
                int skillFeeId = rset.getInt("skill_fee_id");
                int skillFeeAmount = rset.getInt("skill_fee_amount");
                int buffGroup = rset.getInt("buff_group");

                addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
                skillCount++;
            }
        }
        catch (Exception e)
        {
            System.out.println("NpcBufferTable: Error reading npc_buffer table: " + e);
        }

        System.out.println("NpcBufferTable: Loaded " + skillCount + " skills.");
    }

    public static NpcBufferTable getInstance()
    {
        if (_instance == null)
            _instance = new NpcBufferTable();

        return _instance;
    }
}
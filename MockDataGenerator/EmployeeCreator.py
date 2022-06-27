from Data.Branches import *
import numpy as np
from collections import namedtuple
from scipy.stats.distributions import norm

from RandomNameGenerator import RandomNameGenerator

Employee = namedtuple(
    "Employee", "id first_name last_name title reports_to sec_clearance branch salary")


class IDCounter:
    def __init__(self):
        self.__value = 0

    def value(self):
        self.__value += 1
        return self.__value


def create_employees(worker_count):
    engineers_per_branch = (branches.population /
                            total_population * float(worker_count)).to_numpy()

    bms_per_branch = np.maximum(
        1, engineers_per_branch / 50).round().astype(int)

    engineers_per_branch = engineers_per_branch.round().astype(int)

    counter = IDCounter()
    random_name_generator = RandomNameGenerator(
        './Data/first_names.csv', './Data/last_names.csv')
    ceo = __create_ceo(counter)
    cms = __create_country_managers(ceo, counter)
    rms = __create_regional_managers(cms, counter)
    bms = __create_branch_managers(
        rms, counter, bms_per_branch.round().astype(int))
    engineers = __create_branch_workers(bms, counter, engineers_per_branch)

    staff = ceo + cms + rms + bms + engineers
    staff_count = len(staff)
    staff_first_names, staff_last_names = random_name_generator.generate(
        staff_count)

    for (index, (first_name, last_name)) in enumerate(zip(staff_first_names, staff_last_names)):
        staff[index] = list(staff[index])
        staff[index][1] = first_name
        staff[index][2] = last_name

    return staff


def __create_ceo(counter):
    return [Employee(counter.value(), "", "", "CEO", None, 4, 1, int(norm.rvs(loc=5000000, scale=100000)))]


def __create_country_managers(ceo, counter):
    return [
        Employee(counter.value(), "", "", "Country Manager", boss.id, 4, branch[0],
                 int(norm.rvs(loc=2000000, scale=50000)))
        for branch in head_branches.itertuples()
        for boss in ceo
    ]


def __create_regional_managers(cms, counter):
    regions = branches[branches.groupby(
        ['country', 'state']).population.transform(max) == branches.population]

    return [
        Employee(counter.value(), "", "", "Regional Manager", boss.id, 3, branch[0],
                 int(norm.rvs(loc=1000000, scale=30000)))
        for boss in cms
        for branch in regions[regions.country == branches.loc[boss.branch].country].itertuples()
    ]


def __create_branch_managers(rms, counter, bm_counts):
    return [
        Employee(counter.value(), "", "", "Branch Manager", boss.id, 3, branch[0],
                 int(norm.rvs(loc=8000000, scale=30000)))
        for boss in rms
        for branch in branches[
            (branches.country == branches.loc[boss.branch].country)
            & (branches.state == branches.loc[boss.branch].state)
        ].itertuples()
        for _ in range(bm_counts[branch[0] - 1])
    ]


def __create_branch_workers(bms, counter, engineer_counts):
    bm_map = {}
    for bm in bms:
        if bm.branch in bm_map:
            bm_map[bm.branch].append(bm.id)
        else:
            bm_map[bm.branch] = [bm.id]

    return [
        Employee(counter.value(), "", "", "Engineer", int(np.random.choice(bm_map[index+1], 1)[0]), int(np.random.choice([2, 3], 1, [0.9, 0.1])[0]),
                 index+1, int(norm.rvs(loc=6000000, scale=30000)))
        for index, e_count in enumerate(engineer_counts)
        for _ in range(e_count)
    ]


if __name__ == '__main__':
    create_employees()
